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
      regionName: 'XORAZM'
      districtId: ''
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
      console.log(regions)
      for region in regions
        region.y = Math.floor(Math.random() * 100)
        region.cnt = Math.floor(Math.random() * 10000)
        region.abs = Math.floor(Math.random() * 1000)
        region.click = (e) ->
          vm.selected.regionName(e.dataPoint.name.toUpperCase())
          reloadDistrictsChart(e.dataPoint.id)
        region.from0to14 = Math.floor(Math.random() * 100)
        region.from15to17 = Math.floor(Math.random() * 100)
        region.from18 = Math.floor(Math.random() * 100)
      vm.regions(regions)
      loadAllDistricts()

  loadAllDistricts = ->
    $.get(apiUrl.districts)
    .fail handleError
    .done (districts) ->
      vm.districts districts
      loadRegionsChart()
      reloadDistrictsChart(12)

  reloadDistrictsChart = (regionId) ->
    districts = ko.utils.arrayFilter(vm.districts(), (district) -> district.regionId is regionId)
    vm.selected.districts(districts)
    for district in vm.selected.districts()
      district.y = Math.floor(Math.random() * 100)
      district.cnt = Math.floor(Math.random() * 10000)
      district.abs = Math.floor(Math.random() * 1000)
      district.click = (e) ->
        vm.selected.districtId(e.dataPoint.id)
        console.log(e)
      district.from0to14 = Math.floor(Math.random() * 100)
      district.from15to17 = Math.floor(Math.random() * 100)
      district.from18 = Math.floor(Math.random() * 100)
      loadDistricsChart(districts)

  loadRegionsChart = ->
    chart = new CanvasJS.Chart("regionsChart", {
      animationEnabled: true
      legend:
        verticalAlign: "bottom"
        horizontalAlign: "center"
      data: [
        {
          type: "pie"
          showInLegend: true
          toolTipContent: "{name}: <strong>{y}%</strong><br>
                           Jami: <b>{cnt}</b><br>
                           Absolyut soni: <b>{abs}</b><br>
                           Shundan:
                           <ul>
                             <li>0 - 14: <b>{from0to14}</b></li>
                             <li>15 - 17: <b>{from15to17}</b></li>
                             <li>18 - va undan kattalar: <b>{from18}</b></li>
                           </ul>"
          indexLabel: "{name} {cnt}/{y}"
          dataPoints: vm.regions()
        }
      ]
    })
    chart.render()

  loadDistricsChart = (districts) ->
    chart = new CanvasJS.Chart("districtsChart", {
      animationEnabled: true
      theme: "theme1"
      data: [
        {
          type: "doughnut"
          showInLegend: true
          toolTipContent: "{name}: <strong>{y}%</strong><br>
                           Jami: <b>{cnt}</b><br>
                           Absolyut soni: <b>{abs}</b><br>
                           Shundan:
                           <ul>
                             <li>0 - 14: <b>{from0to14}</b></li>
                             <li>15 - 17: <b>{from15to17}</b></li>
                             <li>18 - va undan kattalar: <b>{from18}</b></li>
                           </ul>"
          indexLabel: "{name} {cnt}/{y}%"
          dataPoints: districts
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

  loadAllRegions()

  ko.applyBindings {vm}
