$ ->
  my.initAjax()
  window.Glob ?= {}

#  $('.app-select').selectpicker()

  apiUrl =
    regions: '/home/regions'
    districts: '/home/districts'
    patient: '/card-index/patient'
    patients: '/card-index/patients'
    clientGroups: '/card-index/client-groups'

  handleError = (error) ->
    vm.isLoading(no)
    if error.status is 401
      logout()
    else if error.status is 200 or error.status is 400 and error.responseText
      alert(error.responseText)
    else
      alert('Something went wrong! Please try again.')

  $addPatientModal = $('#add-patient-modal')
  $editPatientModal = $('#edit-patient-modal')

  $.mask.definitions['9'] = ''
  $.mask.definitions['d'] = '[0-9]'
  $('#phoneNumber').mask('998(dd)-ddd-dd-dd');

  $('#birthDate').datetimepicker
    viewMode: 'years'
    format: 'DD.MM.YYYY'
    autoclose: true
    todayHighlight: true

  $patientForm = $('#patient-form')
  fileData = null
  $patientForm.fileupload
    dataType: 'text'
    autoUpload: no
    replaceFileInput: true
    singleFileUploads: true
    multipart: true
    add: (e, data) ->
      fileData = data
    fail: (e, data) ->
      handleError(data.jqXHR)
#      vm.enableSubmitButton(yes)
    done: (e, data) ->
      result = data.result
      if result is 'OK'
        vm.isLoading(no)
        toastr.success('Yangi foydalanuvchi muvaffaqiyatli yaratildi')
        loadAllPatients()
        $addPatientModal.modal('hide')
      else
        alert(result or 'Something went wrong! Please try again.')

  defaultPatient =
    id: ''
    createdAt: ''
    firstName: ''
    lastName: ''
    middleName: ''
    gender: ''
    birthDate: ''
    regionId: ''
    districtId: ''
    clientGroupId: ''
    email: ''
    phoneNumber: ''
    clientGroup:
      id: ''
      name: ''
      code: ''
    district:
      id: ''
      name: ''
      regionId: ''
    patientDataJson:
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
    clientGroups: []
    bloodGroups: ['I(+)', 'I(-)', 'II(+)', 'II(-)', 'III(+)', 'III(-)', 'IV(+)', 'IV(-)']
    selected:
      patient: defaultPatient
    isLoading: no
    isAddingPatient: no
    isUpdatingPatient: no

  vm.formatDate = (millis, format = 'YYYY-MM-DD') ->
    if millis
      moment(millis).format(format)

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
    vm.isAddingPatient(yes)
    ko.mapping.fromJS(defaultPatient, {}, vm.selected.patient)
    $addPatientModal.modal('show')

  vm.onClickEditPatientButton = ->
    vm.isUpdatingPatient(yes)
    #    ko.mapping.fromJS(patient, {}, vm.selected.patient)
    $editPatientModal.modal('show')

  vm.onAddPatient = ->
    patientObj = ko.mapping.toJS(vm.selected.patient)
    if isPatientValid(patientObj)
#      vm.isLoading(yes)
      if fileData
        fileData.submit()
      else
        console.log('!!!!!!!!')
        $patientForm.fileupload('send', {files: ''})
    no

#  vm.editPatient = ->
#    $patientForm.fileupload('send', {files: ''})
#    console.log($patientForm.fileupload('send', {files: ''}))
#    no
  #      $.ajax
#        url: apiUrl.patient
#        data: JSON.stringify(patientObj)
#        type: 'POST'
#        dataType: "json"
#        contentType: 'application/json'
#      .fail handleError
#      .done (id) ->
#        vm.isLoading(no)
#        patientObj.id = id
#        toastr.success('Yangi foydalanuvchi muvaffaqiyatli yaratildi')
#        loadAllPatients()
#        $addPatientModal.modal('hide')

  vm.onPatientSelected = (patient) ->
    ko.mapping.fromJS(patient, {}, vm.selected.patient)
    vm.selected.patient.createdAt(vm.formatDate(vm.selected.patient.createdAt()))
    vm.selected.patient.birthDate(vm.formatDate(vm.selected.patient.birthDate()))
    console.log(patient.gender)
    console.log(vm.selected.patient.gender())

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

  loadAllClientGroups = ->
    $.get(apiUrl.clientGroups)
    .fail handleError
    .done (clientGroups) ->
      vm.clientGroups clientGroups

  vm.selected.patient.regionId.subscribe (regionId) ->
    if regionId and !vm.selected.districtId
      $.get("#{apiUrl.districts}/#{regionId}")
      .fail handleError
      .done (districts) ->
        vm.districts districts

  loadAllPatients()
  loadAllRegions()
  loadAllClientGroups()

  Glob.vm = vm

  ko.applyBindings {vm}
