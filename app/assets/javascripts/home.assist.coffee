$ ->
  my.initAjax()
  Glob = window.Glob || {}

  Links =
    indicator: 'indicator'
    reception: 'reception'
    cardIndex: 'cardIndex'
    statistics: 'statistics'
    settings: 'settings'

  Names =
    indicator: 'Ko\'rsatkichlar'
    reception: 'Qabul'
    cardIndex: 'Bemorlar'
    statistics: 'Statistika'
    settings: 'Sozlash'

  Titles =
    indicator: 'Xonqa'
    reception: 'Qabul jurnali'
    cardIndex: 'Bemorlar'
    statistics: 'Statistika'
    settings: 'Sozlash'

  Descriptions =
    indicator: 'tuman bo\'yicha umumiy kasallik ko\'rsatkichlari'
    reception: 'bemorlar ko\'rigi va ko\'rsatmalar'
    cardIndex: 'kasallik kartalari va bemorlar to\'g\'risidagi batafsil ma\'lumotlar'
    statistics: 'umumiy hisobotlar'
    settings: '...'

  vm = ko.mapping.fromJS
    selectedLink: Links.indicator
    selectedName: Names.indicator
    selectedTitle: Titles.indicator
    selectedDesc: Descriptions.indicator

  vm.Links = Links

  vm.clickLink = (link) -> ->
    vm.selectedLink(link)
    switch link
      when 'indicator'
        vm.selectedName(Names.indicator)
        vm.selectedTitle(Titles.indicator)
        vm.selectedDesc(Descriptions.indicator)
      when 'reception'
        vm.selectedName(Names.reception)
        vm.selectedTitle(Titles.reception)
        vm.selectedDesc(Descriptions.reception)
      when 'cardIndex'
        vm.selectedName(Names.cardIndex)
        vm.selectedTitle(Titles.cardIndex)
        vm.selectedDesc(Descriptions.cardIndex)
      when 'statistics'
        vm.selectedName(Names.statistics)
        vm.selectedTitle(Titles.statistics)
        vm.selectedDesc(Descriptions.statistics)
      when 'settings'
        vm.selectedName(Names.settings)
        vm.selectedTitle(Titles.settings)
        vm.selectedDesc(Descriptions.settings)

  ko.applyBindings {vm}
