#!/bin/bash

# 编译 Java 代码
mvn compile

# 检查编译是否成功
if [ $? -ne 0 ]; then
    echo "编译失败，请检查代码和依赖。"
    exit 1
fi

# 执行 Java 程序
mvn exec:java -Dexec.mainClass="com.example.auc.AucWebSocketDemo" -Dexec.args="appKey accessKey filePath"
