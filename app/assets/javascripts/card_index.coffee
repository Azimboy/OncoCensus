$ ->
  my.initAjax()
  window.Glob ?= {}

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
      alert('Tizimda xatolik! Iltimos qaytadan urinib ko\'ring.')

  $addPatientModal = $('#add-patient-modal')
  $updatePatientModal = $('#update-patient-modal')

#  $('#birthDate').datetimepicker
#    viewMode: 'years'
#    format: 'DD.MM.YYYY'
#    autoclose: true
#    todayHighlight: true

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

#  TODO Need to fix form redirect issue
  formData = {}
  $patientForm = $('#patient-form')
  $patientForm.fileupload
    dataType: 'text'
    autoUpload: false
    replaceFileInput: true
    singleFileUploads: false
    multipart: true
    add: (e, data) ->
      formData = data
    fail: (e, data) ->
      handleError(data.jqXHR)
      vm.isLoading(no)
    done: (e, data) ->
      result = data.result
      if result is 'OK'
        vm.isLoading(no)
        if vm.isNewPatient()
          toastr.success('Yangi ma\'lumotlar ro\'yhatga olindi.')
          $addPatientModal.modal('hide')
        else
          toastr.success('Ma\'lumotlar o\'zgartirildi va saqlandi.')
          $patientForm.modal('hide')
        loadAllPatients()
        vm.isNewPatient(no)
      else
        alert(result or 'Tizimda xatolik! Iltimos qaytadan urinib ko\'ring.')

  $patientForm.submit (e) ->
    e.preventDefault()
    vm.isLoading(yes)
    if isPatientValid(vm.selected.patient)
      if formData
        formData.submit()
      else
        $patientForm.fileupload('send', {files: ''})
      yes
    else
      no

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
      districts: []
    isLoading: no
    isNewPatient: no

  vm.formatDate = (millis, format = 'DD.MM.YYYY') ->
    if millis
      moment(millis).format(format)

  notvalid = (str) ->
    !$.trim(str)

  isPatientValid = (patient) ->
    warningText =
      if notvalid(patient.firstName())
        'Bemorning ismini kiriting!'
      else if notvalid(patient.lastName())
        'Bemorning familiyasini kiriting!'
      else if notvalid(patient.gender())
        'Bemorning jinsini tanlang!'
      else if notvalid(patient.birthDate())
        'Bemorning tug\'ulgan sanasini kiriting!'
      else if notvalid(patient.clientGroupId())
        'Bemorning klient guruhini tanlang!'
      else if notvalid(patient.patientDataJson.bloodGroup())
        'Bemorning qon guruhini tanlang!'
      else if notvalid(patient.patientDataJson.passportNo())
        'Bemorning passport raqamini kiriting!'
      else if notvalid(patient.regionId())
        'Viloyatni tanlang!'
      else if notvalid(patient.districtId())
        'Tumanni tanlang!'
      else if notvalid(patient.patientDataJson.province())
        'Mahalla nomini kiriting!'
      else if notvalid(patient.phoneNumber())
        'Bemorning telefon raqamini kiriting!'

    if warningText
      toastr.error(warningText)
      no
    else
      yes

  vm.onPatientSelected = (patient) ->
    ko.mapping.fromJS(patient, {}, vm.selected.patient)
    vm.selected.patient.createdAt(vm.formatDate(patient.createdAt))
    vm.selected.patient.birthDate(vm.formatDate(patient.birthDate))
    vm.selected.patient.regionId(patient.district.regionId)

  vm.onClickAddPatient = ->
    vm.isNewPatient(yes)
    ko.mapping.fromJS(defaultPatient, {}, vm.selected.patient)
    $addPatientModal.modal('show')

  vm.onClickUpdatePatient = ->
    vm.isNewPatient(no)
    if vm.selected.patient.id()
      $updatePatientModal.modal('show')
    else
      toastr.warning('Tahrirlash uchun bemorni tanlang!')

  vm.onClickRemovePatient = ->
    if vm.selected.patient.id()
      if confirm("Bu bemorning ma'lumotlarni o'chirish xohlaysizmi?")
        $.ajax
          url: apiUrl.patient + "/#{vm.selected.patient.id()}"
          type: 'DELETE'
          dataType: 'json'
        .fail handleError
        .done () ->
          toastr.success("Bemorning ma'lumotlari muvaffaqiyatli o'chirildi.")
          loadAllPatients()
    else
      toastr.warning('O\'chirish uchun bemorni tanlang!')

  loadAllPatients = (event, page) ->
    pageParam = "pageSize=#{pageSize}"
    if page
      pageParam += "&page=#{page}"

    $.get(apiUrl.patients + "?#{pageParam}")
    .fail handleError
    .done (result) ->
      $pagination.destroy?()
      initPagination(result.total, page)
      console.log(result.total)
      vm.patients result.items

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

  loadAllClientGroups = ->
    $.get(apiUrl.clientGroups)
    .fail handleError
    .done (clientGroups) ->
      vm.clientGroups clientGroups

  vm.selected.patient.regionId.subscribe (regionId) ->
    if regionId
      vm.selected.districts(ko.utils.arrayFilter(vm.districts(), (district) -> district.regionId is regionId))

  loadAllPatients()
  loadAllRegions()
  loadAllDistricts()
  loadAllClientGroups()

  Glob.vm = vm

  ko.applyBindings {vm}
