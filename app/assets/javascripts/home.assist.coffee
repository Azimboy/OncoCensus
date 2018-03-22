$ ->
  my.initAjax()
  Glob = window.Glob || {}

  Pages = {
    'indicator': {
      link: 'indicator'
      name: 'Ko\'rsatkichlar'
      title: 'Xonqa'
      desc: 'tuman bo\'yicha umumiy kasallik ko\'rsatkichlari'
    }
    'reception': {
      link: 'reception'
      name: 'Qabul'
      title: 'Qabul jurnali'
      desc: 'bemorlar ko\'rigi va ko\'rsatmalar'
    }
    'cardIndex': {
      link: 'cardIndex'
      name: 'Bemorlar'
      title: 'Bemorlar'
      desc: 'kasallik kartalari va bemorlar to\'g\'risidagi batafsil ma\'lumotlar'
    }
    'statistics': {
      link: 'statistics'
      name: 'Statistika'
      title: 'Statistika'
      desc: 'umumiy hisobotlar'
    }
    'settings': {
      link: 'settings'
      name: 'Sozlash'
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
