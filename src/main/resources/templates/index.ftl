<#import "common/layout.ftl" as rhc>
<@rhc.layout title="Rhc Builder">
    <div class="layui-form layui-form-pane" style="padding: 12px">
        <div class="layui-form-item">
            <div class="layui-inline">
                <label class="layui-form-label">环境</label>
                <div class="layui-input-inline">
                    <input type="hidden" class="layui-input" name="env">
                    <input type="text" class="layui-input" placeholder="请选择环境" id="env" name="env_select" lay-verify="required">
                </div>
            </div>
            <div class="layui-inline">
                <label class="layui-form-label">应用</label>
                <div class="layui-input-inline">
                    <input type="text" class="layui-input" placeholder="请选择应用" id="work" name="work" lay-verify="required">
                </div>
            </div>
        </div>
        <div class="layui-form-item">
            <label class="layui-form-label">是否构建</label>
            <div class="layui-input-block">
                <input type="checkbox" name="build" lay-skin="switch" lay-filter="switchTest" title="构建|不构建">
            </div>
        </div>
        <div class="layui-form-item">
            <div class="layui-input-block" style="margin-left: 0">
                <button type="submit" class="layui-btn" lay-submit lay-filter="start">开始执行</button>
            </div>
        </div>
        <div class="layui-timeline layui-border layui-code-item-preview">
            <div class="layui-timeline-item">
                <i class="layui-icon layui-timeline-axis layui-icon-reduce-circle" id="jenkins_login"></i>
                <div class="layui-timeline-content layui-text">
                    <div class="layui-timeline-title">jenkins登录</div>
                </div>
            </div>
            <div class="layui-timeline-item">
                <i class="layui-icon layui-timeline-axis layui-icon-reduce-circle" id="jenkins_build"></i>
                <div class="layui-timeline-content layui-text">
                    <div class="layui-timeline-title">Jenkins构建</div>
                </div>
            </div>
            <div class="layui-timeline-item">
                <i class="layui-icon layui-timeline-axis layui-icon-reduce-circle" id="jenkins_download"></i>
                <div class="layui-timeline-content layui-text">
                    <div class="layui-timeline-title">构建产物下载</div>
                </div>
            </div>
            <div class="layui-timeline-item">
                <i class="layui-icon layui-timeline-axis layui-icon-reduce-circle" id="git_clone"></i>
                <div class="layui-timeline-content layui-text">
                    <div class="layui-timeline-title">Git仓库拉取</div>
                </div>
            </div>
            <div class="layui-timeline-item">
                <i class="layui-icon layui-timeline-axis layui-icon-reduce-circle" id="copy_file"></i>
                <div class="layui-timeline-content layui-text">
                    <div class="layui-timeline-title">文件拷贝</div>
                </div>
            </div>
            <div class="layui-timeline-item">
                <i class="layui-icon layui-timeline-axis layui-icon-reduce-circle" id="git_commit_and_push"></i>
                <div class="layui-timeline-content layui-text">
                    <div class="layui-timeline-title">Git提交</div>
                </div>
            </div>
            <div class="layui-timeline-item">
                <i class="layui-icon layui-timeline-axis layui-icon-reduce-circle" id="success"></i>
                <div class="layui-timeline-content layui-text">
                    <div class="layui-timeline-title">处理完毕,请前往行云平台查看 <a href="https://steam.crcloud.com"></a></div>
                </div>
            </div>
        </div>
    </div>
    <script>
        layui.use(function(){
            var dropdown = layui.dropdown;
            var form = layui.form;
            $('#env').val('');
            $('#work').val('');

            function getWorkList(env) {
                $.ajax('/api/info/get/'+env).then(res=> {
                    const data = res.data;
                    data.map(item=>{
                        item.title = item.name;
                        return item;
                    })
                    dropdown.render({
                        elem: '#work',
                        closeOnClick: false, // 不开启“打开与关闭的自动切换”，即点击输入框时始终为打开状态
                        data: data,
                        click: function(obj){
                            this.elem.val(obj.title);
                        },
                        style: 'min-width: 235px;'
                    });
                })
            }

            dropdown.render({
                elem: '#env',
                closeOnClick: false, // 不开启“打开与关闭的自动切换”，即点击输入框时始终为打开状态
                data: [{
                    title: 'UAT环境',
                    id: 'uat'
                },{
                    title: 'sit环境',
                    id: 'sit'
                },{
                    title: '生产环境',
                    id: 'prod'
                }],
                click: function(obj){
                    this.elem.val(obj.title);
                    $('[name=env]').val(obj.id)
                    $('#work').val('');
                    getWorkList(obj.id)
                },
                style: 'min-width: 235px;'
            });

            // 提交事件
            form.on('submit(start)', function(data){
                var field = data.field; // 获取表单字段值
                // 显示填写结果，仅作演示用
                console.log(field);
                workflow(field);
                return false; // 阻止默认 form 跳转
            });

            function workflow(data){
                SingletonStart(data);
                //JenkinsLogin();
                //JenkinsBuild(data);
            }


        });
    </script>
</@rhc.layout>