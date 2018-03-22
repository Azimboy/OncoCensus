$ ->
  my.initAjax()
  Glob = window.Glob || {}

  Pages = {
    'indicator': {
      link: 'indicator'
      name: ['Ko\'rsatkichlar']
      title: 'Xonqa'
      desc: 'tuman bo\'yicha umumiy kasallik ko\'rsatkichlari'
    }
    'reception': {
      link: 'reception'
      name: ['Qabul']
      title: 'Qabul jurnali'
      desc: 'bemorlar ko\'rigi va ko\'rsatmalar'
    }
    'cardIndex': {
      link: 'cardIndex'
      name: ['Bemorlar']
      title: 'Bemorlar'
      desc: 'kasallik kartalari va bemorlar to\'g\'risidagi batafsil ma\'lumotlar'
    }
    'patientReports': {
      link: 'patientReports'
      name: ['Hisobotlar', 'Bemorlar']
      title: 'Bemorlar'
      desc: 'umumiy ma\'lumotlar'
    }
    'checkUpReports': {
      link: 'checkUpReports'
      name: ['Hisobotlar', 'Tibbiy ko\'riklar']
      title: 'Tibbiy ko\'rik'
      desc: 'umumiy ma\'lumotlar'
    }
    'settings': {
      link: 'settings'
      name: ['Sozlash']
      title: 'Sozlash'
      desc: '...'
    }
  }

  vm = ko.mapping.fromJS
    selected:
      page: Pages['indicator']

  vm.openPage = (link) -> ->
    ko.mapping.fromJS(Pages[link], {}, vm.selected.page)

  ko.applyBindings {vm}
