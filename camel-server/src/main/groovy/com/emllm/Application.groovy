package com.emllm

import org.apache.camel.main.Main

groovy.transform.CompileStatic
class Application {
    static void main(String[] args) {
        def main = new Main(Application.class)
        main.addRouteBuilder(new EmailProcessingRoute())
        main.run(args)
    }
}
