$ ->
  my.initAjax()
  window.Glob ?= {}

  apiUrl =
    regions: '/home/regions'
    districts: '/home/districts'
    getReport: '/statistics/report'

  handleError = (error) ->
    vm.isLoading(no)
    if error.status is 401
      logout()
    else if error.status is 200 or error.status is 400 and error.responseText
      alert(error.responseText)
    else
      alert('Tizimda xatolik! Iltimos qaytadan urinib ko\'ring.')

  pageSize = 8
  $pagination = {}
  $paginationEl = $('#pagination')

  initPagination = (total, startPage = 1) ->
    totalPages = Math.ceil(total / pageSize)
    if totalPages < 1
      return no
    $paginationEl.show()
    $paginationEl.twbsPagination(
      startPage: Math.min(startPage, totalPages)
      totalPages: totalPages
      visiblePages: 5
      first: ''
      prev: 'Oldingi'
      next: 'Keyingi'
      last: ''
      onPageClick: (event, page) ->
        getReport(null, page)
    )
    $pagination = $paginationEl.data('twbsPagination')

  startDatetime = moment("00:00", 'HH:mm').format('DD.MM.YYYY HH:mm')
  endDatetime = moment("23:59", 'HH:mm').format('DD.MM.YYYY HH:mm')

  initDatePicker = (selector, defaultDate, format) ->
    $el = $(selector)
    $el.on('dp.hide', () ->
      $el.find('input').change()
    )

    $el.datetimepicker
      format: format or 'DD.MM.YYYY HH:mm'
      useCurrent: no
      defaultDate: defaultDate

  initDatePicker('#startDate', startDatetime)
  initDatePicker('#endDate', endDatetime)

  defaultReportData =
    startDate: ''
    endDate: ''

  vm = ko.mapping.fromJS
    regions: []
    districts: []
    reports: []
    reportData:
      startDate: ''
      endDate: ''
      regionId: ''
      districtId: ''
      receiveType: ''
    selected:
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

  vm.reportData.regionId.subscribe (regionId) ->
    if regionId
      vm.selected.districts(ko.utils.arrayFilter(vm.districts(), (district) -> district.regionId is regionId))

  loadAllRegions()
  loadAllDistricts()

  getReport = (event, page) ->
    pageParam = "pageSize=#{pageSize}"
    if page
      pageParam += "&page=#{page}"

    reportDataJs = ko.mapping.toJS(vm.reportData)
    if notvalid(reportDataJs.regionId)
      reportDataJs.regionId = undefined
    if notvalid(reportDataJs.districtId)
      reportDataJs.districtId = undefined
    console.log(reportDataJs)

    $.post(apiUrl.getReport + "?#{pageParam}", JSON.stringify(reportDataJs))
    .fail handleError
    .done (report) ->
      $pagination.destroy?()
      initPagination(report.total, page)
#      reportItems = report.items
#      for item in reportItems
#         = vm.getAge(patient.birthDate)
      vm.reports report.items

  vm.reportData.receiveType.subscribe ->
    getReport()

  vm.reportData.startDate.subscribe ->
    getReport()

  vm.reportData.endDate.subscribe ->
    getReport()

  getReport()

  vm.reportData.regionId.subscribe () ->
    getReport()

  vm.reportData.districtId.subscribe ->
    getReport()

  Glob.vm = vm

  ko.applyBindings {vm}
