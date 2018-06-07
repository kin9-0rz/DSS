# DSS(dexsim-server)

dexsim 的 Android服务端，通过动态加载的方式，为 dexsim 提供解密服务。

## 为什么

主要是因为在静态解密时，遇到Context等于Android OS相关的类就无法处理。

## FAQ

**Q. java.io.FileNotFoundException: /data/local/dss_data/od-output.json: open failed: EACCES (Permission denied)**

A：删除该文件再试。
