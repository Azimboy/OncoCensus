$ ->
  my.initAjax()
  Glob = window.Glob || {}

  Links =
    indicator: 'indicator'
    reception: 'reception'
    patients: 'patients'
    statistics: 'statistics'

  Names =
    indicator: 'Ko\'rsatkichlar'
    reception: 'Qabul'
    patients: 'Bemorlar'
    statistics: 'Statistika'

  Titles =
    indicator: 'Xonqa'
    reception: 'Qabul jurnali'
    patients: 'Bemorlar'
    statistics: 'Statistika'

  Descriptions =
    indicator: 'tuman bo\'yicha umumiy kasallik ko\'rsatkichlari'
    reception: 'bemorlar ko\'rigi va ko\'rsatmalar'
    patients: 'kasallik kartalari va bemorlar to\'g\'risidagi batafsil ma\'lumotlar'
    statistics: 'umumiy hisobotlar'

  vm = ko.mapping.fromJS
    selectedLink: Links.indicator
    selectedName: Names.indicator
    selectedTitle: Titles.indicator
    selectedDesc: Descriptions.indicator

  vm.Links = Links

  vm.clickLink = (link) -> ->
    vm.selectedLink(link)
    console.log(link)
    switch link
      when 'indicator'
        vm.selectedName(Names.indicator)
        vm.selectedTitle(Titles.indicator)
        vm.selectedDesc(Descriptions.indicator)
      when 'reception'
        vm.selectedName(Names.reception)
        vm.selectedTitle(Titles.reception)
        vm.selectedDesc(Descriptions.reception)
      when 'patients'
        vm.selectedName(Names.patients)
        vm.selectedTitle(Titles.patients)
        vm.selectedDesc(Descriptions.patients)
      when 'statistics'
        vm.selectedName(Names.statistics)
        vm.selectedTitle(Titles.statistics)
        vm.selectedDesc(Descriptions.statistics)

  ko.applyBindings {vm}
