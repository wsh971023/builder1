$.ajaxSetup({
    async : false //让ajax同步
});

/**
 * 修改状态 icon
 * @param id
 * @param status
 */
const changeStatus = (id, status) =>{
    console.log(id,status);
    const $item = $('#'+id);
    $item.removeClass();
    if (status===Status.SUCCESS){
        $item.addClass('layui-icon layui-timeline-axis rhc-success');
    }else if (status===Status.FAIL){
        $item.addClass('layui-icon layui-timeline-axis rhc-fail');
    }else if (status===Status.RUNNING){
        $item.addClass('layui-icon layui-timeline-axis layui-anim layui-anim-rotate layui-anim-loop layui-icon-loading-1');
    }else {
        $item.addClass('layui-icon layui-timeline-axis layui-icon-reduce-circle');
    }
}

const GetJobStatus = (data) => {
    $.post({
        url: '/api/status/get',
        contentType: 'application/json',
        data: JSON.stringify(data),
        dataType: 'json',
        success: function(res) {
            if (res.data){
                const processList = res.data.statusInfo;
                console.log("processList:",processList);
                let status = true;
                for (const info of processList) {
                    console.log(info.status);
                    console.log(info.status === Status.SUCCESS);
                    if (info.status === Status.SUCCESS){
                        console.log("info:",info);
                        changeStatus(info.process, Status.SUCCESS);
                    }else if (info.status === Status.RUNNING){
                        status = false;
                        changeStatus(info.process, Status.RUNNING);
                    }else if (info.status === Status.FAIL){
                        status = false;
                        changeStatus(info.process, Status.FAIL);
                    }else if (info.status === Status.INIT){
                        status = false;
                        changeStatus(info.process, Status.INIT);
                    }
                }
                if (status){
                    changeStatus("success", Status.SUCCESS)
                }else{
                    setTimeout(()=>{GetJobStatus(data)}, 2500)
                }
            }else{
                setTimeout(()=>{GetJobStatus(data)}, 2500)
            }
        },
        error: function(res) {
            setTimeout(()=>{GetJobStatus(data)}, 2500)
        }
    });
}

const SingletonStart = (data) => {
    $.post({
        url: '/api/task/submit',
        contentType: 'application/json',
        data: JSON.stringify(data),
        dataType: 'json',
        success: (res)=>{
            setTimeout(()=>{GetJobStatus(data)}, 10000)
        }
    });
}
