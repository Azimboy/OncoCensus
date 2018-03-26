$ ->
  my.initAjax()
  window.Glob ?= {}

  apiUrl =
    regions: '/home/regions'
    districts: '/home/districts'

  handleError = (error) ->
    vm.isLoading(no)
    if error.status is 401
      logout()
    else if error.status is 200 or error.status is 400 and error.responseText
      alert(error.responseText)
    else
      alert('Tizimda xatolik! Iltimos qaytadan urinib ko\'ring.')

  vm = ko.mapping.fromJS
    regions: []
    districts: []
    selected:
      regionId: ''
      districts: []
    isLoading: no

  vm.formatDate = (millis, format = 'DD.MM.YYYY HH:mm') ->
    if millis
      moment(millis).format(format)

  notvalid = (str) ->
    !$.trim(str)

  loadAllRegions = ->
    $.get(apiUrl.regions)
    .fail handleError
    .done (regions) ->
      vm.regions regions

  loadAllDistricts = ->
    $.get(apiUrl.districts)
    .fail handleError
    .done (districts) ->
      vm.selected.districts districts
      vm.districts districts

  vm.selected.regionId.subscribe (regionId) ->
    if regionId
      vm.selected.districts(ko.utils.arrayFilter(vm.districts(), (district) -> district.regionId is regionId))

  loadAllRegions()
  loadAllDistricts()

  loadRegionsChart = ->
    chart = new CanvasJS.Chart("regionsChart", {
      exportFileName: "Viloyatlar bo'yicha"
      exportEnabled: true
      animationEnabled: true
      legend:
        verticalAlign: "bottom"
        horizontalAlign: "center"
      data: [
        {
          type: "pie"
          showInLegend: true
          toolTipContent: "{legendText}: <strong>{y}%</strong>"
          indexLabel: "{label} {y}%"
          dataPoints: [
            { y: 35, legendText: "Andijon", exploded: true, label: "Andijon" }
            { y: 20, legendText: "Buxoro", label: "Buxoro" }
            { y: 18, legendText: "Farg'ona", label: "Farg'ona" }
            { y: 15, legendText: "Jizzax", label: "Jizzax" }
            { y: 5, legendText: "Xorazm", label: "Xorazm" }
            { y: 7, legendText: "Namangan", label: "Namangan" }
            { y: 7, legendText: "Navoiy", label: "Navoiy" }
            { y: 7, legendText: "Qashqadaryo", label: "Qashqadaryo" }
            { y: 7, legendText: "Qoraqalpog'iston Respublikasi", label: "Qoraqalpog'iston Respublikasi" }
            { y: 7, legendText: "Samarqand", label: "Samarqand" }
            { y: 7, legendText: "Toshkent", label: "Toshkent" }
          ]
        }
      ]
    })
    chart.render()

  loadDistricsChart = ->
    chart = new CanvasJS.Chart("districtsChart", {
      exportFileName: "Tumanlar bo'yicha"
      exportEnabled: true
      animationEnabled: true
      theme: "theme1"
      data: [
        {
          type: "doughnut"
          showInLegend: true
#          indexLabelFontFamily: "Garamond"
#          indexLabelFontSize: 20
#          startAngle: 0
#          indexLabelFontColor: "dimgrey"
#          indexLabelLineColor: "darkgrey"
          toolTipContent: "{legendText}: <strong>{y}%</strong>"
          dataPoints: [
            { y: 51.04, exploded: true, legendText: "Bog'ot", indexLabel: "Bog'ot {y}%" }
            { y: 40.83, legendText: "Gurlan", indexLabel: "Gurlan {y}%" }
            { y: 3.20, legendText: "Xonqa", indexLabel: "Xonqa {y}%" }
            { y: 1.11, legendText: "Hazorasp", indexLabel: "Hazorasp {y}%" }
            { y: 2.29, legendText: "Xiva", indexLabel: "Xiva {y}%" }
            { y: 4.53, legendText: "Qo'shko'pir", indexLabel: "Qo'shko'pir {y}%" }
            { y: 1.53, legendText: "Shovot", indexLabel: "Shovot {y}%" }
            { y: 6.53, legendText: "Urganch", indexLabel: "Urganch {y}%" }
            { y: 7.53, legendText: "Yangiariq", indexLabel: "Yangiariq {y}%" }
            { y: 9.53, legendText: "Yangibozor", indexLabel: "Yangibozor {y}%" }
          ]
        }
      ]
    })
    chart.render()

  loadRegionsChart()
  loadDistricsChart()

  ko.applyBindings {vm}
