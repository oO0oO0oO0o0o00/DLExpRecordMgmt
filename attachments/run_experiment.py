import argparse
import gc
import json
from shutil import copyfile
from datetime import datetime

from loguru import logger

import directories_config as dirconf, config_loader
from commons import *

from cuspage import CustomPageHandler
from util import meow_utils
from util.heartbeat import ProgressWritingHeartBeat

# import matplotlib

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '1'
SAVE_KEY_TEST_SCORES = 'test_scores'
SAVE_KEY_TRAINING_HISTORY = 'training_history'


def __main__():
    # heartbeat timestamps allow external programs to monitor the training progress
    heart_beat = ProgressWritingHeartBeat(config, interval_secs=10)
    config.progress = 'Running'
    config.completed = False
    heart_beat.daemon = True
    heart_beat.start()

    # save summary of the experiment
    with open(pjoin(config.home, "summary.json"), 'w') as fp:
        json.dump({
            'config_file': config.filename,
            'config_name': config.name,
            'config_intro': config.intro,
            'start_time': config.start_time,
            'num_fold': config.num_fold
        }, fp, indent=4)

    # make a copy of the current version of the config file as it may be changed in the future
    copyfile(config.path, pjoin(config.home, 'config.py'))

    exception = None
    # run the experiment
    try:
        run_experiment_and_save_results()
    except (Exception, KeyboardInterrupt) as e:
        exception = e
        if DEBUG:
            raise exception

    # stop heartbeat timer and write the completed status
    heart_beat.stop()
    if exception is not None:
        if isinstance(exception, KeyboardInterrupt):
            config.progress = 'Aborted'
            heart_beat.beat()
            logger.error("stopped by user")
        else:
            config.progress = 'Error'
            heart_beat.beat()
            logger.opt(exception=exception).error("failed:")
    else:
        config.progress = 'Completed'
        config.completed = True
        heart_beat.beat()


def run_experiment_and_save_results():
    run_experiment_of_ith_fold_method = run_experiment_of_ith_fold_methods[config.framework]
    for i in range(config.num_fold):
        logger.info("begin fold {}/{}", i + 1, config.num_fold)
        results = run_experiment_of_ith_fold_method(i)
        save_results_of_ith_fold(*results)


def run_experiment_of_ith_fold_keras(ith_fold):
    from tensorflow.python.framework import ops as tffw_ops
    tffw_ops.reset_default_graph()
    gc.collect()
    stages = getattr(config, "stages", None)
    if stages is None:
        return run_experiment_of_jth_stage_of_ith_fold_keras(
            ith_fold=ith_fold, jth_stage=None, model_config=config.model_config
        )
    else:
        histories, test_scores_es = {}, {}
        for jth_stage, stage_config in enumerate(stages):
            histories_of_jth_stage, test_scores_es_of_jth_stage = \
                run_experiment_of_jth_stage_of_ith_fold_keras(
                    ith_fold=ith_fold, jth_stage=jth_stage, model_config=stage_config
                )
            histories |= histories_of_jth_stage
            test_scores_es |= test_scores_es_of_jth_stage
        return histories, test_scores_es


def run_experiment_of_ith_fold_sklearn(ith_fold):
    gc.collect()
    train_set, valid_set, test_sets = config.load_data(ith_fold)
    assert len(train_set) == 2 and len(test_sets) > 0
    assert valid_set is None or len(valid_set) == 2
    model = config.build_model()
    model.fit(*train_set)
    history = config.calculate_metrics(train_set[1], model.predict(train_set[0]))
    if valid_set is not None:
        history |= {f"val_{k}": v for k, v in
                    config.calculate_metrics(valid_set[1], model.predict(valid_set[0])).items()}
    # mimic the curve of epochs by repeating the results twice (otherwise a single dot is not visually comfortable)
    history = {k: [v, v] for k, v in history.items()}
    scores = {}
    test_names = getattr(config, "test_names", None)
    if test_names is None:
        test_names = [None for _ in test_sets]
    else:
        assert len(test_sets) == len(test_names)
    for test_name, test_set in zip(test_names, test_sets):
        for metric_name, metric_val in config.calculate_metrics(test_set[1], model.predict(test_set[0])).items():
            scores[meow_utils.add_prefix_to_metric_name(metric_name, test_name)] = metric_val
    return history, scores


run_experiment_of_ith_fold_methods = {
    "keras": run_experiment_of_ith_fold_keras,
    "sklearn": run_experiment_of_ith_fold_sklearn
}


def run_experiment_of_jth_stage_of_ith_fold_keras(*, ith_fold, jth_stage, model_config):
    if jth_stage is not None:
        kwargs = {'stage': jth_stage}
    else:
        kwargs = {}
    train_set, valid_set, test_sets = config.load_data(ith_fold, **kwargs)
    # build the models
    # the model can consist of many building blocks (sub-models)
    model, components = config.build_model(**kwargs)
    # plot the diagrams of the sub-models (may include the overall model) and save as pictures
    if ith_fold == 0:
        log_models_summary(components)

    # a basic learning rate scheduling
    total_epochs, lr_scheduler = meow_keras_utils.build_learning_rate_scheduler_given_config(
        model_config['schedule'])
    if total_epochs is None:
        total_epochs = model_config['epochs']

    # to save best models automatically
    checkpoints_path = pjoin(config.home, "checkpoints", meow_utils.zfill(ith_fold, 2))
    if jth_stage is not None:
        checkpoints_path = pjoin(checkpoints_path, model_config["name"])
    model_checkpoint_callback = meow_keras_utils.build_model_checkpoint_callback(
        ensure_mkdir(checkpoints_path, parent=False),
        monitor=model_config.get("monitor_metric"),
        mode=model_config.get("monitor_max_or_min")
    )

    generic_callbacks = [model_checkpoint_callback]
    if lr_scheduler is not None:
        generic_callbacks.append(lr_scheduler)

    train_x, train_t = meow_keras_utils.expand_dataset_for_keras_fit_function(train_set)

    fit_callbacks = getattr(config, "get_fit_callbacks", lambda *_: [])(
        train_set, valid_set, CustomPageHandler(config.home, ith_fold=ith_fold))

    # train the model and get the history, with validation after each epoch
    history = model.fit(
        train_x, train_t, validation_data=valid_set,
        batch_size=model_config.get('batch_size'), epochs=total_epochs,
        callbacks=generic_callbacks + model_config['callbacks'] + fit_callbacks
    )

    # evaluate the model on test sets
    scores = {}
    if test_sets is not None:
        meow_keras_utils.load_final_weights(model, checkpoints_path)
        test_names = model_config.get("test_names")
        if test_names is None:
            test_names = [None for _ in test_sets]
        else:
            assert len(test_sets) == len(test_names)
        for test_name, test_set in zip(test_names, test_sets):
            test_x, test_t = meow_keras_utils.expand_dataset_for_keras_fit_function(test_set)
            scores |= {meow_utils.add_prefix_to_metric_name(k, test_name): v
                       for k, v in model.evaluate(test_x, test_t, return_dict=True).items()}

    history = history.history
    if "lr" in history:
        del history['lr']
    if jth_stage is not None:
        metric_prefix = model_config["prefix"]
        history = {meow_utils.add_prefix_to_metric_name(k, metric_prefix): v for k, v in history.items()}
        scores = {meow_utils.add_prefix_to_metric_name(k, metric_prefix): v for k, v in scores.items()}

    return history, scores


def log_models_summary(models):
    models_dir = ensure_mkdir(pjoin(config.home, "models"), parent=False)
    from tensorflow import keras
    for model_key, (_, model) in models.items():
        keras.utils.plot_model(model, to_file=pjoin(models_dir, f"{model_key}.png"), show_shapes=True, dpi=192)

    summary_path = pjoin(models_dir, "models.json")
    if os.path.exists(summary_path):
        with open(summary_path, 'r') as fp:
            existing = json.load(fp)
    else:
        existing = {}
    with open(summary_path, 'w') as fp:
        json.dump(
            existing | {model_key: model_name for model_key, (model_name, _) in models.items()},
            fp, indent=4
        )


def save_results_of_ith_fold(history, scores):
    results_path = pjoin(config.home, "results.json")
    if os.path.exists(results_path):
        with open(results_path, 'r') as fp:
            all_results = json.load(fp)
    else:
        all_results = {SAVE_KEY_TRAINING_HISTORY: [], SAVE_KEY_TEST_SCORES: []}
    all_results[SAVE_KEY_TEST_SCORES].append(scores)
    all_results[SAVE_KEY_TRAINING_HISTORY].append(history)
    with open(results_path, 'w') as fp:
        json.dump(all_results, fp, indent=4)


def parse_debug_arg():
    parser = argparse.ArgumentParser()
    parser.add_argument("-d", action='store_true', help="enables debug mode")
    args, _ = parser.parse_known_args()
    return args.d


if __name__ == '__main__':
    DEBUG = parse_debug_arg()
    config = config_loader.load_config()
    config.start_time = datetime.today().strftime('%y%m%d-%H%M%S')
    config.home = ensure_mkdir(pjoin(dirconf.EXPERIMENTS_PATH, config.start_time), parent=False)
    logger.add(pjoin(config.home, "log.txt"), backtrace=True, diagnose=True)
    logger.info(f"begin experiment with config {config.filename}")
    # matplotlib.use('Agg')
    config.use_tf = config.framework == "keras"
    if config.use_tf:
        from util import meow_keras_utils

        meow_keras_utils.limit_gpu_mem_usage(config.gpu_mem_limit_mb)
    __main__()
