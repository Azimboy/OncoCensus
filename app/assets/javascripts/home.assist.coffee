$ ->
  my.initAjax()
  Glob = window.Glob || {}

  Links =
    indicator: 'indicator'
    reception: 'reception'

  Pages =
    'indicator':
      name: 'Ko\'rsatkichlar'
      title: 'Xonqa'
      description: 'tuman bo\'yicha umumiy kasallik ko\'rsatkichlari'
    'reception':
      name: 'Qabul'
      title: 'Be\'morlar'
      description: 'kasallik kartalari va bemorlar to\'g\'risida batafsil ma\'lumotlar'

  vm = ko.mapping.fromJS
    selectedLink: Links.indicator
    selectedPage: Pages[Links.indicator]

  vm.Links = Links

  vm.clickLink = (link) -> ->
    vm.selectedLink(link)
    vm.selectedPage = Pages[link]
    console.log(vm.selectedPage.title)

  ko.applyBindings {vm}
