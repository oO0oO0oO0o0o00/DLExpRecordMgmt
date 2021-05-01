var navDetailPanelsVue;


function capitalizeFirstChar(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}


function displayTrainingChartOfMetric(name, train, valid) {
    let $chartWrapper = $('#training-charts-template-wrapper .chart-wrapper').clone();
    $('#training-charts-host').append($chartWrapper);
    $chartWrapper.find('.chart-title').text(name.split('_')
        .map(capitalizeFirstChar).join(' '));

    name = name.replace('_', ' ');
    let data = {
        type: 'line',
        data: {
            datasets: [{
                label: "Training",
                data: train,
                fill: false,
                backgroundColor: '#f00',
                borderColor: '#f00'
            }, {
                label: "Validation",
                data: valid,
                fill: false,
                backgroundColor: '#00f',
                borderColor: '#00f'
            }], labels: [...train.keys()]
        },
        options: {
            responsive: true,
        }
    };
    let chart = new Chart($chartWrapper.find('canvas')[0].getContext('2d'), data);
}

function displayTrainingCharts(data) {
    for (let key of Object.keys(data)) {
        if (key.startsWith('val_')) continue;
        displayTrainingChartOfMetric(key, data[key], data[`val_${key}`])
    }
}

function loadDetailsPageTrainingCharts($pageElement) {
    $.ajax({
        url: $pageElement.attr('href'),
        dataType: 'json',
        success(data, status, xhr) {
            displayTrainingCharts(data);
        }
    })
}

function displayModelsSummary(url, data){
    for (let key of Object.keys(data)) {
        let $wrapper = $('#models-summary-template-wrapper div').clone();
        $('#models-summary-host').append($wrapper);
        $wrapper.find('.img-title').text(data[key].split(' ').map(capitalizeFirstChar).join(' '));
        let $img = $wrapper.find('img');
        $img.attr('src', `${url}/${key}`)
        $img.attr('alt', `structure of ${data[key]}`)
    }
}

function loadDetailsPageModels($pageElement) {
    let url = $pageElement.attr('href');
    $.ajax({
        url: url,
        dataType: 'json',
        success(data, status, xhr) {
            displayModelsSummary(url, data);
        }
    })
}

function loadDetailsPagePredictionVisualization(pageElement) {
    pageElement.text('33');
}

function loadDetailsPageConfigFile(pageElement) {
    pageElement.text('44');
}

function loadDetailsPage(tab) {
    if (!tab.hasClass('not-loaded')) return;
    tab.removeClass('not-loaded');
    let selector = tab.attr('data-bs-target');
    let target = $(selector)
    switch (selector) {
        case '#nav-details-training':
            loadDetailsPageTrainingCharts(target);
            break;
        case '#nav-details-models':
            loadDetailsPageModels(target);
            break;
        case '#nav-details-prediction':
            loadDetailsPagePredictionVisualization(target);
            break;
        case '#nav-details-config':
            loadDetailsPageConfigFile(target);
            break;
    }
}

$(document).ready(e => {
    let $sb = $('#top-search-bar-input');
    $sb.on('focusin', e => $(e.target).attr('placeholder', "这tm有啥好搜索的，憨批"));
    $sb.on('focusout', e => $(e.target).attr('placeholder', "猪"));

    $('#error-time-too-old').attr('title',
        'The experiment han\'t updated its progress for more than 10 minutes and is likely failed. ' +
        'Difference in time and time zone settings between the host of this platform and the ' +
        'host where the experiments run on may also cause the problem.')
    $('#error-time-too-young').attr('title',
        'The last time the experiment reported its progress is ahead of this machine\'s local time. ' +
        'Time and time zone settings between the host of this platform and the host where the ' +
        'experiments run on shall be identical.')
    $('[data-toggle="tooltip"]').tooltip()

    $('#nav-detail-panels .nav-item').on('show.bs.tab', function (e) {
        loadDetailsPage($(this));
    })
    loadDetailsPage($('#nav-detail-panels .nav-item.active'))
});
