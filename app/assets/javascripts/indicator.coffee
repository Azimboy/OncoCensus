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
#      title:
#        text: "Viloyatlar bo'yicha kasallik ko'rsatkichlari"
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
          toolTipContent: "{legendText}: <strong>{y}%</strong><br>
                           Jami: <b>155</b><br>
                           Absolyut soni: <b>20</b><br>
                           Shundan:
                           <ul>
                             <li>0 - 14: <b>2</b></li>
                             <li>15 - 17: <b>5</b></li>
                             <li>18 - va undan kattalar: <b>6</b></li>
                           </ul>"
          indexLabel: "{legendText} 655/{y}%"
          dataPoints: [
            { y: 35, legendText: "Andijon", exploded: true }
            { y: 20, legendText: "Buxoro" }
            { y: 18, legendText: "Farg'ona" }
            { y: 15, legendText: "Jizzax" }
            { y: 5, legendText: "Xorazm" }
            { y: 7, legendText: "Namangan" }
            { y: 7, legendText: "Navoiy" }
            { y: 7, legendText: "Qashqadaryo" }
            { y: 7, legendText: "Qoraqalpog'iston Respublikasi" }
            { y: 7, legendText: "Samarqand" }
            { y: 7, legendText: "Toshkent" }
          ]
        }
      ]
    })
    chart.render()

  loadDistricsChart = ->
    chart = new CanvasJS.Chart("districtsChart", {
#      title:
#        text: "Tumanlar bo'yicha kasallik ko'rsatkichlari"
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
          toolTipContent: "{legendText}: <strong>{y}%</strong><br>
                           Jami: <b>155</b><br>
                           Absolyut soni: <b>20</b><br>
                           Shundan:
                           <ul>
                             <li>0 - 14: <b>2</b></li>
                             <li>15 - 17: <b>5</b></li>
                             <li>18 - va undan kattalar: <b>6</b></li>
                           </ul>"
          indexLabel: "{legendText} 156/{y}%"
          dataPoints: [
            { y: 51.04, exploded: true, legendText: "Bog'ot" }
            { y: 40.83, legendText: "Gurlan" }
            { y: 3.20, legendText: "Xonqa" }
            { y: 1.11, legendText: "Hazorasp" }
            { y: 2.29, legendText: "Xiva" }
            { y: 4.53, legendText: "Qo'shko'pir" }
            { y: 1.53, legendText: "Shovot" }
            { y: 6.53, legendText: "Urganch" }
            { y: 7.53, legendText: "Yangiariq" }
            { y: 9.53, legendText: "Yangibozor" }
          ]
        }
      ]
    })
    chart.render()

  loadPatientsStatsChart = ->
    chart = new CanvasJS.Chart("patientsStatsChart", {
      title:
        text: "Tuman bo'yicha oylik kasallik ko'rsatkichlari"
      theme: "theme2"
      animationEnabled: true
      axisX:
        valueFormatString: "MMMM",
        interval: 1
        intervalType: "month"
      axisY:
        includeZero: false
      data: [
        {
          type: "line"
          lineThickness: 3
          dataPoints: [
            { x: new Date(2018, 0, 1), y: 450 },
            { x: new Date(2018, 1, 1), y: 414 },
            { x: new Date(2018, 2, 1), y: 520, markerColor: "red", markerType: "triangle" },
            { x: new Date(2018, 3, 1), y: 460 },
            { x: new Date(2018, 4, 1), y: 450 },
            { x: new Date(2018, 5, 1), y: 500 },
            { x: new Date(2018, 6, 1), y: 480 },
            { x: new Date(2018, 7, 1), y: 480 },
            { x: new Date(2018, 8, 1), y: 410, markerColor: "DarkSlateGrey", markerType: "cross" },
            { x: new Date(2018, 9, 1), y: 500 },
            { x: new Date(2018, 10, 1), y: 480 },
            { x: new Date(2018, 11, 1), y: 510 }
          ]
        }
      ]
    })
    chart.render()

  loadRegionsChart()
  loadDistricsChart()
  loadPatientsStatsChart()

  ko.applyBindings {vm}
