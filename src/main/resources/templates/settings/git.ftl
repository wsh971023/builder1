<#import "../common/layout.ftl" as rhc>
<@rhc.layout title="Rhc Builder Settings">
    <div class="layui-form layui-form-pane" style="padding: 12px">
        <div class="layui-form-item">
            <label class="layui-form-label">用户名</label>
            <div class="layui-input-block">
                <input type="text" name="username" value="${config.username}" lay-verify="required" placeholder="" autocomplete="off" class="layui-input">
            </div>
        </div>
        <div class="layui-form-item">
            <label class="layui-form-label">Token</label>
            <div class="layui-input-block">
                <input type="text" name="token" value="${config.token}" lay-verify="required" placeholder="" autocomplete="off" class="layui-input">
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
                    url: '/settings/git/save',
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