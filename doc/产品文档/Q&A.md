1. PL/SQL DEBUG过程中由于机器睡眠报错：**This socket has been closed**。

   1. 问题原因：
      debug过程中，由于机器休眠导致jdbc的自动断掉连接，继续使用该连接，在checkSession的时候会报：**this socket has been closed**。

   2. 解决办法：
      关闭当前debug， 重启debug流程。

