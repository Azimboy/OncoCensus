$ ->
  my.initAjax()
  window.Glob ?= {}

  apiUrl =
    regions: '/home/regions'
    districts: '/home/districts'
    patient: '/card-index/patient'
    patients: '/card-index/patients'

  handleError = (error) ->
    vm.isLoading(no)
    if error.status is 401
      logout()
    else if error.status is 200 or error.status is 400 and error.responseText
      alert(error.responseText)
    else
      alert('Something went wrong! Please try again.')

  birthDate = moment("00:00", 'hh:mm A').format('DD-MM-YYYY hh:mm A')

  initDatePicker = (selector, defaultDate, format) ->
    $el = $(selector)
    $el.on('dp.hide', () ->
      $el.find('input').change()
    )

    $el.datetimepicker
      format: format or 'YYYY-MM-DD hh:mm A'
      useCurrent: no
      defaultDate: defaultDate

  initDatePicker('#dateFrom', birthDate)

  $addPatientModal = $('#add-patient-modal')
  $editPatientModal = $('#edit-patient-modal')

  defaultPatient =
    firstName: ''
    lastName: ''
    middleName: ''
    gender: ''
    birthDate: ''
    regionId: ''
    districtId: ''
    email: ''
    phoneNumber: ''
    passportNo: ''
    province: ''
    street: ''
    home: ''
    work: ''
    position: ''
    bloodGroup: ''

  vm = ko.mapping.fromJS
    patients: []
    regions: []
    districts: []
    selected:
      patient: defaultPatient
    isLoading: no

  notvalid = (str) ->
    !$.trim(str)

  isPatientValid = (patient) ->
    warningText =
      if notvalid(patient.firstName)
        'Ism maydonini  to\'ldiring'
      else if notvalid(patient.lastName)
        'Familiya maydonini to\'ldiring'
      else if notvalid(patient.middleName)
        'Otasining ismi maydonini to\'ldiring'
      else if !patient.districtId
        'Tuman maydonini to\'ldiring'
      else if patient.email and !my.isValidEmail(patient.email)
        'Haqiqiy email manzilini kiriting'
      else if patient.phoneNumber and !my.isValidPhone(patient.phoneNumber)
        'Haqiqiy telefon raqamni kiriting'

    if warningText
      toastr.error(warningText)
      no
    else
      yes

  vm.onClickAddPatientButton = ->
    ko.mapping.fromJS(defaultPatient, {}, vm.selected.patient)
    $addPatientModal.modal('show')

  vm.createPatient = ->
    patientObj = ko.mapping.toJS(vm.selected.patient)
    console.log(patientObj)
    if isPatientValid(patientObj)
      vm.isLoading(yes)
      $.ajax
        url: apiUrl.patient
        data: JSON.stringify(patientObj)
        type: 'POST'
        dataType: "json"
        contentType: 'application/json'
      .fail handleError
      .done (id) ->
        vm.isLoading(no)
        patientObj.id = id
        patientObj.createdAt = +new Date
        toastr.success('Yangi foydalanuvchi muvaffaqiyatli yaratildi')
        loadAllUsers()
        $addPatientModal.modal('hide')

#  vm.onClickEditUserButton = (user) ->
#    ko.mapping.fromJS(user, {}, vm.selected.user)
#    $editUserModal.modal('show')

  #  vm.updateUser = () ->
  #    patientObj = ko.mapping.toJS(vm.selected.user)
  #
  #    if isUserValid(patientObj)
  #      vm.isLoading(yes)
  #      $.ajax
  #        url: apiUrl.user + "/#{patientObj.id}"
  #        data: JSON.stringify(patientObj)
  #        type: 'PUT'
  #        dataType: "json"
  #        contentType: 'application/json'
  #      .fail handleError
  #      .done () ->
  #        vm.isLoading(no)
  #        toastr.success('Muvaffaqiyatli saqlandi')
  #        loadAllUsers()
  #        $editUserModal.modal('hide')

  vm.formatDate = (millis, format = 'MMM DD YYYY') ->
    if millis
      moment(millis).format(format)

  loadAllPatients = ->
    $.get(apiUrl.patients)
    .fail handleError
    .done (patients) ->
      vm.patients patients

  loadAllRegions = ->
    $.get(apiUrl.regions)
    .fail handleError
    .done (regions) ->
      vm.regions regions

  vm.selected.patient.regionId.subscribe (regionId) ->
    if regionId and !vm.selected.districtId
      $.get("#{apiUrl.districts}/#{regionId}")
      .fail handleError
      .done (districts) ->
        vm.districts districts

  loadAllPatients()
  loadAllRegions()

  Glob.vm = vm

  ko.applyBindings {vm}
