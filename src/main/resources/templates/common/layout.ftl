<#macro layout title>
    <!DOCTYPE html>
    <html lang="zh">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${title}</title>
        <link rel="stylesheet" href="/lib/layui/css/layui.css">
        <link rel="stylesheet" href="/css/global.css">
        <link rel="stylesheet" href="/css/index.css">
        <script src="/lib/jquery/jquery.js"></script>
        <script src="/lib/layui/layui.js"></script>
        <script src="/js/Constants.js"></script>
        <script src="/js/Workflow.js"></script>
    </head>
    <body>
    <div class="layui-layout layui-layout-admin">
        <div class="layui-header">
            <div class="layui-logo layui-hide-xs layui-bg-black">Rhc Builder</div>
            <ul class="layui-nav layui-layout-left">
                <li class="layui-nav-item layui-hide-xs"><a href="/">执行</a></li>
                <li class="layui-nav-item">
                    <a href="javascript:;">设置</a>
                    <dl class="layui-nav-child">
                        <dd><a href="/settings/jenkins">Jenkins配置</a></dd>
                        <dd><a href="">Git配置</a></dd>
                        <dd><a href="">代理设置</a></dd>
                    </dl>
                </li>
            </ul>
        </div>
        <div class="layui-body layui-layout-body">
            <#nested/>
        </div>
        <footer class="layui-footer">
            <p>© 2025 rhc builder by cuizhy</p>
        </footer>
    </div>
    </body>
    </html>
</#macro>

