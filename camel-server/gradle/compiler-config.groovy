withConfig(configuration) {
    ast(groovy.transform.CompileStatic)
    ast(groovy.transform.TypeChecked)
    ast(groovy.transform.CompileDynamic, value: false)
}
