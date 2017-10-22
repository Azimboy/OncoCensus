$ ->
  my.initAjax()
  Glob = window.Glob || {}

  Links =
    indicator: 'indicator'
    reception: 'reception'
    patients: 'patients'

  Names =
    indicator: 'Ko\'rsatkichlar'
    reception: 'Qabul'
    patients: 'Bemorlar'

  Titles =
    indicator: 'Xonqa'
    reception: 'Qabul jurnali'
    patients: 'Bemorlar'

  Descriptions =
    indicator: 'tuman bo\'yicha umumiy kasallik ko\'rsatkichlari'
    reception: 'bemorlar ko\'rigi va ko\'rsatmalar'
    patients: 'kasallik kartalari va bemorlar to\'g\'risidagi batafsil ma\'lumotlar'

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

  ko.applyBindings {vm}
