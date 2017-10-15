$ ->
  my.initAjax()
  Glob = window.Glob || {}

  MenuName =
    App: 'app' # Personal Mobile Banking App
    Myspend: 'myspend'
    Socialapp: 'socialapp'
    Lendpers: 'lendpers'
    Econditions: 'econditions'
    Student: 'student'

  defaultMenuName = MenuName.App

  vm = ko.mapping.fromJS
    menuName: defaultMenuName

  console.log(vm.menuName())

  ko.applyBindings {vm}
