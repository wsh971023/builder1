<#import "../common/layout.ftl" as rhc>
<@rhc.layout title="Rhc Builder Settings">
    <div style="color: green;font-weight: bold;padding-top: .5em;padding-left: 12px">当前代理配置信息: ${currentProxy}</div>
    <div class="layui-form layui-form-pane" style="padding: 12px">
        <div class="layui-form-item">
            <label class="layui-form-label">地址</label>
            <div class="layui-input-block">
                <input type="text" name="host" value="${config.host!}" lay-verify="required" placeholder="" autocomplete="off" class="layui-input">
            </div>
        </div>
        <div class="layui-form-item">
            <label class="layui-form-label">端口</label>
            <div class="layui-input-block">
                <input type="text" name="port" value="${config.port!}" lay-verify="required" placeholder="" autocomplete="off" class="layui-input">
            </div>
        </div>
        <div class="layui-form-item">
            <label class="layui-form-label">是否启用</label>
            <div class="layui-input-block">
                <input type="checkbox" name="enabled"  lay-skin="switch" lay-filter="switchTest" title="启用|禁用">
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
            if ("${config.enabled}" == '1'){
                $('[name=enabled]').prop('checked', true);
                layui.form.render();
            }

            layui.form.on('submit(save)', function(data){
                var field = data.field; // 获取表单字段值
                // 显示填写结果，仅作演示用
                console.log(field);
                if (field.enabled === 'on'){
                    field.enabled = 1;
                }else{
                    field.enabled = 0;
                }
                $.post({
                    url: '/settings/proxy/save',
                    contentType: 'application/json',
                    data: JSON.stringify(field),
                    dataType: 'json',
                    success: (res)=>{
                        layer.msg("保存成功,重启后生效",{icon:1,time:10000});
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