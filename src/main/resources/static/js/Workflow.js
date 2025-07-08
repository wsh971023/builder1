// 创建一个控制器对象来管理所有任务相关的逻辑
var rhcTaskController = {
    // 用于存储从 layui.use 传入的模块实例
    element: null,
    form: null,
    dropdown: null,
    $: null,
    layer: null,

    /**
     * 通用Ajax请求封装
     * @param {Object} options 请求参数
     */
    request: function (options) {
        const defaultOptions = {
            method: 'POST',
            url: '',
            data: {},
            contentType: 'application/json',
            dataType: 'json',
            showMsg: true, // 是否自动弹出提示
            beforeSend: null,
            complete: null
        };

        const opt = Object.assign({}, defaultOptions, options);

        return new Promise((resolve, reject) => {
            $.ajax({
                method: opt.method,
                url: opt.url,
                data: opt.contentType === 'application/json' ? JSON.stringify(opt.data) : opt.data,
                contentType: opt.contentType,
                dataType: opt.dataType,
                beforeSend: opt.beforeSend,
                success: function (res) {
                    if (res.code === 200) {
                        resolve(res.data); // 返回数据部分
                    } else {
                        if (opt.showMsg && window.layer) {
                            layer.msg(res.message || '操作失败', { icon: 2 });
                        }
                        reject({ code: res.code, message: res.message || '接口返回错误' });
                    }
                },
                error: function (xhr) {
                    if (opt.showMsg && window.layer) {
                        layer.msg('网络或服务器错误', { icon: 2 });
                    }
                    reject({ code: xhr.status, message: xhr.statusText || '网络异常' });
                },
                complete: opt.complete
            });
        });
    },


    /**
     * 初始化函数，由 FTL 文件中的 layui.use 调用
     * @param {object} modules - 包含所有 Layui 模块的对象
     */
    init: function(modules) {
        this.element = modules.element;
        this.form = modules.form;
        this.dropdown = modules.dropdown;
        this.$ = modules.jquery;
        this.layer = modules.layer;

        // 在这里可以执行一些只需要运行一次的初始化绑定
        this.bindFormSubmit();
        this.initEnvDropdown();
    },

    /**
     * 绑定表单提交事件
     */
    bindFormSubmit: function() {
        // 使用 this.form 来访问模块
        this.form.on('submit(start)', (data) => {
            // 使用 this.SingletonStart 来调用对象内部的方法
            this.SingletonStart(data.field);
            return false; // 阻止默认 form 跳转
        });
    },

    /**
     * 初始化环境下拉菜单和级联逻辑
     */
    initEnvDropdown: function() {
        // 环境下拉菜单
        this.dropdown.render({
            elem: '#env',
            data: [{ title: 'UAT环境', id: 'uat' }, { title: 'SIT环境', id: 'sit' }, { title: '生产环境', id: 'prod' }],
            click: (obj) => {
                this.$('#env').val(obj.title);
                this.$('input[name=env]').val(obj.id);
                this.$('#work').val('');
                // 调用对象内部的方法
                this.getWorkList(obj.id);
            },
            style: 'min-width: 235px;'
        });
    },

    /**
     * 获取并渲染应用列表
     * @param {string} env - 环境ID
     */
    getWorkList: function(env) {

        this.request({
            url: '/api/info/get/' + env,
        }).then(res => {
            const data = res.map(item => {
                item.title = item.name;
                return item;
            });
            this.dropdown.render({
                elem: '#work',
                data: data,
                click: (obj) => {
                    this.$('#work').val(obj.title);
                },
                style: 'min-width: 235px;'
            });
        })
    },

    /**
     * 提交新任务
     */
    SingletonStart: function(data) {

        this.request( {
            url: '/api/task/submit',
            data: data,
        }).then( res => {
            this.layer.msg('任务已提交', { icon: 1 });
            // 提交后立即刷新一次列表，体验更好
            this.GetTaskInfo();
        })
    },

    /**
     * 获取所有任务信息
     */
    GetTaskInfo: function() {
        this.request({
            url: '/api/task/info',
        }).then( res => {
            if (res && res.running_job) {
                const runningJobsObject = res.running_job;
                const tasks = Object.keys(runningJobsObject).map(jobKey => {
                    const jobData = runningJobsObject[jobKey];
                    let overallStatus = '已完成';
                    let status = "success";
                    if (jobData.statusInfo.some(s => s.status.toLowerCase() === 'fail')) { overallStatus = '失败'; status = "fail"; }
                    else if (jobData.statusInfo.some(s => s.status.toLowerCase() === 'running')) { overallStatus = '运行中'; status = "running"; }
                    else if (!jobData.statusInfo.every(s => s.status.toLowerCase() === 'success')) { overallStatus = 'pending'; status = "pending"; }

                    return { taskId: jobKey, env: jobData.env, work: jobData.name, status:status, status_text: overallStatus, steps: jobData.statusInfo };
                });
                this.renderOrUpdateTasks(tasks);
            }
        })
    },

    /**
     * 渲染或更新所有任务的UI (已增强状态显示)
     * @param {Array} tasks
     */
    renderOrUpdateTasks: function(tasks) {
        const container = this.$('#task-list-container');

        tasks.forEach(task => {
            const taskDomId = 'task-' + task.taskId;
            let $taskItem = this.$('#' + taskDomId);

            if ($taskItem.length === 0) {
                const taskHtml = '<div class="layui-colla-item" id="' + taskDomId + '">' +
                    '<h2 class="layui-colla-title"></h2>' +
                    '<div class="layui-colla-content">' + this.createTaskTimelineHtml(task.taskId, task.steps) + '</div>' +
                    '</div>';
                container.prepend(taskHtml);
                $taskItem = this.$('#' + taskDomId); // 创建后重新获取 jQuery 对象
            }

            // 1. 根据状态获取对应的图标HTML
            let statusIconHtml = '';
            switch (task.status) {
                case 'running':
                    statusIconHtml = '<i class="layui-icon layui-icon-loading-1 layui-anim layui-anim-rotate layui-anim-loop status-icon"></i>';
                    break;
                case 'success':
                    statusIconHtml = '<i class="layui-icon layui-icon-ok-circle status-icon"></i>';
                    break;
                case 'fail':
                    statusIconHtml = '<i class="layui-icon layui-icon-close-circle status-icon"></i>';
                    break;
                case 'PENDING':
                    statusIconHtml = '<i class="layui-icon layui-icon-auz status-icon"></i>'; // “待授权”图标，形状像时钟
                    break;
            }

            // 2. 准备标题文本
            const titleText = task.env + ' - ' + task.work + ' [' + task.status_text + ']';

            // 3. 移除旧的状态 class，添加新的 class
            //    这样可以确保颜色总是能正确更新
            const statusClassName = 'rhc-status-' + task.status.toLowerCase();
            $taskItem.removeClass('rhc-status-running rhc-status-success rhc-status-fail rhc-status-pending')
                .addClass(statusClassName);

            // 4. 更新标题的 HTML，包含图标和文本
            //    注意：这里要用 .html() 而不是 .text()，这样图标才能被渲染
            $taskItem.find('.layui-colla-title').html(statusIconHtml + titleText);

            // --- 修改结束 ---

            // 更新内部 timeline 的所有图标状态 (这部分逻辑不变)
            task.steps.forEach(step => {
                const iconId = 'icon-' + task.taskId + '-' + step.process;
                this.updateIconStatus(iconId, step.status);
            });
        });

        // 渲染手风琴
        this.element.render('collapse', 'task-accordion');
    },

    /**
     * 根据任务数据生成其时间线(timeline)的 HTML
     * @param {string} taskId
     * @param {Array} steps
     */
    createTaskTimelineHtml: function(taskId, steps) {
        let timelineHtml = '<div class="layui-timeline" style="padding: 10px 0;">';
        steps.forEach(step => {
            const iconId = 'icon-' + taskId + '-' + step.process;
            timelineHtml += '<div class="layui-timeline-item">' +
                '<i class="layui-icon layui-timeline-axis layui-icon-reduce-circle" id="' + iconId + '"></i>' +
                '<div class="layui-timeline-content layui-text">' +
                '<div class="layui-timeline-title">' + (JobProcessMap[step.process] || step.process) + '</div>' +
                '</div>' +
                '</div>';
        });
        timelineHtml += '</div>';
        return timelineHtml;
    },

    /**
     * 更新指定图标的状态
     * @param {string} iconId
     * @param {string} status
     */
    updateIconStatus: function(iconId, status) {
        const $item = this.$('#' + iconId);
        if (!$item.length) return;
        let iconClass = 'layui-icon layui-timeline-axis ';
        switch (status.toLowerCase()) {
            case 'success': iconClass += 'rhc-success layui-icon-ok-circle'; break;
            case 'fail': iconClass += 'rhc-fail layui-icon-close-circle'; break;
            case 'running': iconClass += 'layui-icon-loading-1 layui-anim layui-anim-rotate layui-anim-loop'; break;
            default: iconClass += 'layui-icon-reduce-circle'; break;
        }
        $item.attr('class', iconClass);
    },

    /**
     * 定时轮询任务状态
     */
    pollTasksStatus: function() {
        this.GetTaskInfo();
        setTimeout(() => this.pollTasksStatus(), 2500);
    }
};