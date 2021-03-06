$ ->
  my.initAjax()
  window.Glob ?= {}

  apiUrl =
    regions: '/home/regions'
    districts: '/home/districts'
    icds: '/home/client-groups'
    patients: '/reports/patients'

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
        loadAllPatients(null, page)
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
    icds: []
    patients: []
    reportData:
      startDate: ''
      endDate: ''
      regionId: ''
      districtId: ''
      receiveType: ''
    selected:
      districts: []
    filters:
      lastName: undefined
      isMale: yes
      isFemale: yes
      minAge: undefined
      maxAge: undefined
      icd: undefined
      passportId: undefined
      province: undefined
    isLoading: no

  vm.formatDate = (millis, format = 'DD.MM.YYYY HH:mm') ->
    if millis
      moment(millis).format(format)

  vm.getAge = (date) ->
    if date
      moment().diff(date, 'years')

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

  loadAllIcds = ->
    $.get(apiUrl.icds)
    .fail handleError
    .done (icds) ->
      vm.icds icds

  vm.reportData.regionId.subscribe (regionId) ->
    if regionId
      vm.selected.districts(ko.utils.arrayFilter(vm.districts(), (district) -> district.regionId is regionId))

  loadAllRegions()
  loadAllDistricts()
  loadAllIcds()

  loadAllPatients = (event, page) ->
    pageParam = "pageSize=#{pageSize}"
    if page
      pageParam += "&page=#{page}"

    filtersJs = ko.mapping.toJS(vm.filters)
    minAge = filtersJs.minAge
    maxAge = filtersJs.maxAge

    if minAge
      filtersJs.minAge = parseInt(minAge)
    if maxAge
      filtersJs.maxAge = parseInt(maxAge)

    $.post(apiUrl.patients + "?#{pageParam}", JSON.stringify(filtersJs))
    .fail handleError
    .done (result) ->
      $pagination.destroy?()
      initPagination(result.total, page)
      patients = result.items
      for patient in patients
        patient.age = vm.getAge(patient.birthDate)
        if patient.supervisedOutJson?.date
          patient.supervisedOutAt = vm.formatDate(parseInt(patient.supervisedOutJson.date))
      #        if patient.supervisedOutJson?.date
      #          patient.supervisedOutJson.date = vm.formatDate(parseInt(patient.supervisedOutJson.date))

      vm.patients patients

  loadAllPatients()

  Glob.vm = vm

  ko.applyBindings {vm}
