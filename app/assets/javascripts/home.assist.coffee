$ ->
  my.initAjax()
  Glob = window.Glob || {}

  MenuName =
    Indicator: 'indicator'
    Reception: 'reception'

  defaultMenuName = MenuName.Indicator

  vm = ko.mapping.fromJS
    menuName: defaultMenuName

  vm.MenuName = MenuName

  vm.setMenuName = (menuName) ->
    vm.menuName(menuName)

  vm.loadMenuIframe = (menuName) -> ko.computed ->
    vm.menuName(menuName)

  ko.applyBindings {vm}
