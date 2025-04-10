<#import "../common/layout.ftl" as rhc>
<@rhc.layout title="Rhc Builder Settings">
    <div class="layui-form layui-form-pane" style="padding: 12px">
        <div class="layui-form-item">
            <label class="layui-form-label">Jenkins地址</label>
            <div class="layui-input-block">
                <input type="text" name="url" value="${config.url}" lay-verify="required" placeholder="" autocomplete="off" class="layui-input">
            </div>
        </div>
        <div class="layui-form-item">
            <label class="layui-form-label">用户名</label>
            <div class="layui-input-block">
                <input type="text" name="username" value="${config.username}" lay-verify="required" placeholder="" autocomplete="off" class="layui-input">
            </div>
        </div>
        <div class="layui-form-item">
            <label class="layui-form-label">密码</label>
            <div class="layui-input-block">
                <input type="text" name="password" value="${config.password}" lay-verify="required" placeholder="" autocomplete="off" class="layui-input">
            </div>
        </div>
        <div class="layui-form-item">
            <label class="layui-form-label">构建API</label>
            <div class="layui-input-block">
                <input type="text" name="build_url" value="${config.build_url}" lay-verify="required" placeholder="触发构建API,{job_name}为固定占位符,不可修改" autocomplete="off" class="layui-input">
            </div>
        </div>
        <div class="layui-form-item">
            <label class="layui-form-label">构建状态API</label>
            <div class="layui-input-block">
                <input type="text" name="build_status_url" value="${config.build_status_url}" lay-verify="required" placeholder="获取构建状态API,{job_name}与{build_number}为固定占位符,不可修改" autocomplete="off" class="layui-input">
            </div>
        </div>
        <div class="layui-form-item">
            <label class="layui-form-label">crumb获取API</label>
            <div class="layui-input-block">
                <input type="text" name="crumb_url" value="${config.crumb_url}" lay-verify="required" placeholder="" autocomplete="off" class="layui-input">
            </div>
        </div>
        <div class="layui-form-item">
            <label class="layui-form-label">Job信息API</label>
            <div class="layui-input-block">
                <input type="text" name="job_info_url" value="${config.job_info_url}" lay-verify="required" placeholder="Job信息API,触发构建API,{job_name}为固定占位符,不可修改" autocomplete="off" class="layui-input">
            </div>
        </div>
        <div class="layui-form-item">
            <label class="layui-form-label">工作空间地址</label>
            <div class="layui-input-block">
                <input type="text" name="workspace_url" value="${config.workspace_url}" lay-verify="required" placeholder="工作空间地址,触发构建API,{job_name}为固定占位符,不可修改" autocomplete="off" class="layui-input">
            </div>
        </div>
        <div class="layui-form-item">
            <div class="layui-input-block" style="margin-left: 0">
                <button type="submit" class="layui-btn" lay-submit lay-filter="save">保存</button>
            </div>
        </div>
    </div>
    <script>
        layui.use(function(){
            layui.form.on('submit(save)', function(data){
                var field = data.field; // 获取表单字段值
                // 显示填写结果，仅作演示用
                console.log(field);
                $.post({
                    url: '/settings/jenkins/save',
                    contentType: 'application/json',
                    data: JSON.stringify(field),
                    dataType: 'json',
                    success: (res)=>{
                        layer.msg("保存成功",{icon:1,time:1000});
                        setTimeout(()=>{
                            location.reload();
                        },1000);
                    }
                });
                return false; // 阻止默认 form 跳转
            });
        })
    </script>
</@rhc.layout>