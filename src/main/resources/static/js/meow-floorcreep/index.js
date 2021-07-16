var navDetailPanelsVue;


function capitalizeFirstChar(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}

function displayTrainingChartOfMetric(name, train, valid) {
    let $chartWrapper = $('#training-charts-template-wrapper .chart-wrapper').clone();
    $('#training-charts-host').append($chartWrapper);
    $chartWrapper.find('.chart-title').text(name.split('_')
        .map(capitalizeFirstChar).join(' '));

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
    // noinspection JSValidateTypes
    new Chart($chartWrapper.find('canvas')[0].getContext('2d'), data);
}

function displayTrainingCharts(data) {
    $('#training-charts-host').html('');
    for (let key of Object.keys(data)) {
        if (key.startsWith('val_')) continue;
        displayTrainingChartOfMetric(key, data[key], data[`val_${key}`])
    }
}

function loadDetailsPageTrainingCharts($pageElement, ithFold) {
    if (!$pageElement) $pageElement = $("#nav-details-training");
    $.ajax({
        url: $pageElement.attr('href'),
        data: {"ith-fold": ithFold},
        dataType: 'json',
        success(data) {
            displayTrainingCharts(data);
        }
    })
}

function setDetailsPageTrainingChartsPage(new_page) {
    // TODO: parameterize this:
    let selector_prefix = '#pagination ';
    // record the old page number, we gonna deactivate it
    let old_page = page;
    // handle previous, next or numbers
    switch (new_page) {
        case '<':
            page -= 1;
            break;
        case '>':
            page += 1;
            break;
        default:
            page = new_page;
            break;
    }
    // fit in range
    page = Math.min(Math.max(1, page), max_page);
    // no "previous" for page one
    if (page === 1)
        setPageEnabled(selector_prefix + getPageNumberSelector('<'), false);
    else
        setPageEnabled(selector_prefix + getPageNumberSelector('<'), true);
    // no "next" for the last page
    if (page === max_page)
        setPageEnabled(selector_prefix + getPageNumberSelector('>'), false);
    else
        setPageEnabled(selector_prefix + getPageNumberSelector('>'), true);
    // deactivate old
    setPageActive(selector_prefix + getPageNumberSelector(old_page), false);
    // activate new
    setPageActive(selector_prefix + getPageNumberSelector(page), true);
    loadDetailsPageTrainingCharts(null, page);
}

function getPageNumberSelector(which) {
    let selector;
    switch (which) {
        case '<':
            selector = '.page-previous';
            break;
        case '>':
            selector = '.page-next';
            break;
        default:
            selector = `.page-number[page-number=${which}]`
            break;
    }
    return selector;
}

function setPageEnabled(selector, enabled) {
    let $selector = $(selector);
    let $linkSelector = $(selector + ' .page-link')
    if (enabled) {
        $selector.removeClass('disabled');
        $linkSelector.removeClass('tabindex');
        $linkSelector.removeAttr('aria-disabled');
    } else {
        $selector.addClass('disabled');
        $linkSelector.attr('tabindex', '-1');
        $linkSelector.attr('aria-disabled', true);
    }
}

function setPageActive(selector, active) {
    let $selector = $(selector);
    if (active) {
        $selector.addClass('active');
        $selector.html($selector.html() + '<span class="sr-only">(current)</span>');
    } else {
        $selector.removeClass('active');
        $(selector + ' .sr-only').remove();
    }
}

function displayModelsSummary(url, data) {
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
    let url = $('#nav-details-models').attr('href');
    let baseUrl = new URL(url, location);
    baseUrl = new URL(baseUrl.searchParams.get("record-id"), baseUrl);
    $.ajax({
        url: url,
        dataType: 'json',
        success(data) {
            displayModelsSummary(baseUrl, data);
        }
    })
}

function loadDetailsPageLogFile($pageElement) {
    $pageElement.load($pageElement.attr('href'));
}

function loadDetailsPageConfigFile($pageElement) {
    $pageElement.load($pageElement.attr('href'));
}

function loadDetailsPage(tab) {
    if (!tab.hasClass('not-loaded')) return;
    tab.removeClass('not-loaded');
    let selector = tab.attr('data-bs-target');
    let target = $(selector)
    switch (selector) {
        case '#nav-details-training':
            // loadDetailsPageTrainingCharts(target);
            if (page == null)
                setDetailsPageTrainingChartsPage(1)
            break;
        case '#nav-details-models':
            loadDetailsPageModels(target);
            break;
        case '#nav-details-log':
            loadDetailsPageLogFile(target);
            break;
        case '#nav-details-config':
            loadDetailsPageConfigFile(target);
            break;
    }
}

function simpleToast(text, config) {
    let $toast = $('#toast-template-wrapper .toast.simple-toast').clone();
    $toast.find('.toast-body').text(text);
    addToast($toast, config);
}

function addToast($toast, config) {
    $('#toast-host').append($toast);
    if (config) $toast.toast(config);
    $toast.toast('show');
    $toast.on('hidden.bs.toast.auto-remove', e => $(e.currentTarget).remove());
}

function requestDeleteWeights(element) {
    let $toast = $('#toast-template-wrapper .toast.deletion-toast').clone();
    addToast($toast);
    $toast.attr('href', element.getAttribute('href'));
    $toast.on('hide.bs.toast', performDeleteWeights);
}

function requestDelete(element) {
    if (confirm("???")) {
        let toastFailure = () => simpleToast("Deletion may have failed", {delay: 2000});
        $.ajax({
            url: element.getAttribute('href'),
            method: 'POST',
            dataType: 'json',
            success(data, status) {
                if (data.status === true && status === 'success') {
                    simpleToast("Deleted");
                } else toastFailure();
            }, error: toastFailure
        })
    }
}

function performDeleteWeights(event) {
    if (event.currentTarget.hasAttribute("cancelled")) return;
    let href = event.currentTarget.getAttribute('href');
    let toastFailure = () => simpleToast("Deletion may have failed", {delay: 2000});
    $.ajax({
        url: href,
        method: 'POST',
        dataType: 'json',
        success(data, status) {
            if (status === 'success' && data.status === true) {
                simpleToast("Deleted");
                $('#delete-weights-area').addClass('d-none');
            } else toastFailure();
        }, error: toastFailure
    })
}

function cancelDeleteWeights(element) {
    while (!element.classList.contains("toast")) element = element.parentElement;
    if (element.hasAttribute("cancelled")) return;
    element.setAttribute("cancelled", "yes");
    simpleToast("Cancelled.");
    $(element).toast('hide');
}

$(document).ready(e => {
    // search bar
    let $sb = $('#top-search-bar-input');
    $sb.on('focusin', e => $(e.target).attr('placeholder', "这tm有啥好搜索的，憨批"));
    $sb.on('focusout', e => $(e.target).attr('placeholder', "猪"));

    // summary panel -> progress -> help
    $('#error-time-too-old').attr('title',
        'The experiment hasn\'t updated its progress for more than 10 minutes and is likely failed. ' +
        'Difference in time and time zone settings between the host of this platform and the ' +
        'host where the experiments run on may also cause the problem.')
    $('#error-time-too-young').attr('title',
        'The last time the experiment reported its progress is ahead of this machine\'s local time. ' +
        'Time and time zone settings between the host of this platform and the host where the ' +
        'experiments run on shall be identical.')
    $('[data-toggle="tooltip"]').tooltip()

    // detail panels -> handle loading && load default (first) page
    $('#nav-detail-panels .nav-item').on('show.bs.tab', function () {
        loadDetailsPage($(this));
    })
    loadDetailsPage($('#nav-detail-panels .nav-item.active'));

    let vueEditCoordinatesModalAreaSelect = new Vue({
        el: '#table-digits-range',
        data: {
            digits: 3
        },
        methods: {
            change() {
                for (let row of $('#summary-table tbody tr')) {
                    let $row = $(row);
                    let values = $row.find('td .digits').slice(1).toArray()
                        .map(e => parseFloat($(e).attr('data-value')))
                        .filter(e => !isNaN(e))
                    let mean = values.reduce((a, b) => a + b, 0.0) / values.length;
                    let std = Math.sqrt(values.map(e => e - mean).map(e => e * e)
                        .reduce((a, b) => a + b, 0.0) / values.length);
                    // console.log("" + mean + "+" + std);
                    let $meanAndStd = $row.find('td:first-of-type .digits');
                    $($meanAndStd[0]).attr('data-value', "" + mean);
                    $($meanAndStd[1]).attr('data-value', "" + std);
                }
                $('#summary-table .digits').each((i, e) => {
                    $e = $(e);
                    let num = Number($e.attr('data-value'));
                    $e.text(isNaN(num) ? '-' : num.toFixed(this.$data.digits));
                })
            }
        },
        created() {
            this.change();
        },
    });
    vueEditCoordinatesModalAreaSelect.$data.digits = 3;

    // left bar -> scroll to current
    $('#left-bar .active')[0].scrollIntoView({block: "center"})
});
