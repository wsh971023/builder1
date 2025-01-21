<#macro layout title>
    <!DOCTYPE html>
    <html lang="zh">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${title}</title>
        <link rel="stylesheet" href="lib/layui/css/layui.css">
        <script src="lib/jquery/jquery.js"></script>
        <script src="lib/layui/layui.js"></script>
        <script src="js/Constants.js"></script>
        <script src="js/Workflow.js"></script>
    </head>
    <body class="layui-layout-body">
    <#nested/>
    <footer>
        <p>© 2025 rhc builder by cuizhy</p>
    </footer>
    </body>
    </html>
</#macro>

