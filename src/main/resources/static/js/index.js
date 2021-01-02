function setPage(new_page) {
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
    loadContent(page);
}

function palette(which) {
    return ['#f44336', '#9C27B0',
        '#3F51B5', '#03A9F4',
        '#009688', '#8BC34A',
        '#FFC107', '#FF5722',
        '#795548', '#212121'
    ][which];
}

function onGotContent(data, status, xhr) {
    let i = 1;
    for (let itemName of Object.keys(data)) {
        let $item = $(`#charts-${i}`);
        if ($item.length <= 0) {
            console.error("Cannot find enough container for this data.")
            return;
        }
        let html = `<h3>${itemName}</h3>\n`
        let item = data[itemName];
        let j = 0;
        for (let stageName of Object.keys(item)) {
            html += `<h5>${stageName}</h5>\n`
            html += `<div class="chart-container"><canvas id="chart-${i}-${j}"></canvas></div>`
            j++;
        }
        $item.html(html);
        j = 0;
        for (let stageName of Object.keys(item)) {
            let stage = item[stageName];
            let data = {
                type: 'line',
                data: {
                    // labels: ['Red', 'Blue', 'Yellow', 'Green', 'Purple', 'Orange'],
                    datasets: []
                },
                options: {
                    responsive: true,
                    // scales: {
                    //     yAxes: [{
                    //         ticks: {
                    //             beginAtZero: true
                    //         }
                    //     }]
                    // }
                }
            };
            let k = 0;
            for (let metricName of Object.keys(stage)) {
                if (metricName === 'lr') continue;
                let metric = stage[metricName];
                data.data.datasets.push({
                    label: metricName,
                    data: metric,
                    fill: false,
                    backgroundColor: palette(k),
                    borderColor: palette(k)
                })
                k++;
            }
            let labels = []
            for (k in stage[Object.keys(stage)[0]]) labels.push(k);
            data.data.labels = labels;
            let chart = new Chart(
                document.getElementById(`chart-${i}-${j}`).getContext('2d'),
                data);
            j++;
        }
        i++;
    }
}

function loadContent(page) {
    // <div class="chart-container">
    //     <canvas id="chart"></canvas>
    // </div>
    let $req = $('#req-detail-href');
    $.ajax({
        url: $req.attr('href'),
        method: 'GET',
        data: {
            record: $req.attr('data'),
            ith: page - 1
        },
        dataType: 'json',
        success: onGotContent
    });
    // chart = echarts.init(document.getElementById('chart'));
    //
    // // 指定图表的配置项和数据
    // let option = {
    //     responsive: true,
    //     maintainAspectRatio: true,
    //     aspectRatio: 1,
    //     title: {
    //         text: 'ECharts 入门示例'
    //     },
    //     tooltip: {},
    //     legend: {
    //         data: ['销量']
    //     },
    //     xAxis: {
    //         data: ["衬衫", "羊毛衫", "雪纺衫", "裤子", "高跟鞋", "袜子"]
    //     },
    //     yAxis: {},
    //     series: [{
    //         name: '销量',
    //         type: 'bar',
    //         data: [5, 20, 36, 10, 10, 20]
    //     }]
    // };
    //
    // // 使用刚指定的配置项和数据显示图表。
    // chart.setOption(option);
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

$(document).ready(e => {
    if (page !== null) setPage(page);
    let $sb = $('#top-search-bar-input');
    $sb.on('focusin', e => $(e.target).attr('placeholder', "这tm有啥好搜索的，憨批"));
    $sb.on('focusout', e => $(e.target).attr('placeholder', "猪"));
});
