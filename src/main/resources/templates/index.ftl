<#import "common/layout.ftl" as rhc>
<@rhc.layout title="Rhc Builder">
    <div class="layui-form layui-form-pane" style="padding: 12px">
        <fieldset class="layui-elem-field">
            <legend>提交新任务</legend>
            <div class="layui-field-box">
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
                    <div class="layui-form-item">
                        <label class="layui-form-label">是否构建</label>
                        <div class="layui-input-block">
                            <input type="checkbox" name="build" lay-skin="switch" lay-filter="switchTest" title="构建|不构建">
                        </div>
                    </div>
                    <div class="layui-inline">
                        <button type="submit" class="layui-btn" lay-submit lay-filter="start">开始执行</button>
                    </div>
                </div>
            </div>
        </fieldset>
    </div>

    <fieldset class="layui-elem-field" style="margin: 12px">
        <legend>正在运行的任务列表</legend>
        <div class="layui-field-box">
            <div id="task-list-container" class="layui-collapse" lay-filter="task-accordion">
            </div>
        </div>
    </fieldset>
    <script>
        // 加载所有需要的模块
        layui.use(['element', 'form', 'dropdown', 'jquery', 'layer'], function() {
            // 将模块打包成一个对象
            var modules = {
                element: layui.element,
                form: layui.form,
                dropdown: layui.dropdown,
                jquery: layui.jquery,
                layer: layui.layer
            };

            // **核心步骤：初始化我们的控制器，并将模块注入进去**
            rhcTaskController.init(modules);
            // 页面加载后，启动轮询
            rhcTaskController.layer.msg('正在加载任务列表...', { icon: 16, time: 1000, shade: 0.1 });
            rhcTaskController.pollTasksStatus();
        });
    </script>
</@rhc.layout>