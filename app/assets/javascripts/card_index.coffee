$ ->
  my.initAjax()
  window.Glob ?= {}

  apiUrl =
    regions: '/home/regions'
    districts: '/home/districts'
    patient: '/card-index/patient'
    patients: '/card-index/patients'
    clientGroups: '/card-index/client-groups'
    checkUps: '/card-index/check-ups'

  complaints = ['Umumiy holsizlik', 'Qabziyat', 'Bel-dumg\'aza sohasida og\'riq', 'Ishtaha pastligi', 'Epitsistostomik naycha borligi']
  statuses = ['Qorin simmetrik', 'Nafas aktida qatnashadi', 'Paypaslaganda yumshoq', 'Qorin pastki qismida epitsistostomik naycha', 'Funksiyasi saqlangan', 'Periferik l\tugunlari kattalashmagan']

  $addPatientModal = $('#add-patient-modal')
  $updatePatientModal = $('#update-patient-modal')
  $addCheckUpModal = $('#add-medical-check-modal')

#  $('.date-picker').datepicker
#    autoclose: true
#    todayHighlight: true

#  $('.date-time-picker').datetimepicker
##    format: 'MM.DD.YYYY hh:mm:ss'
#    autoclose: true
#    todayHighlight: true

  tagComplaint = $('#complaint')
  tagStatusLocalis = $('#statusLocalis')
  try
    tagComplaint.tag
      placeholder: '...'
      source: complaints
    tagStatusLocalis.tag
      placeholder: '...'
      source: statuses
  catch e
    tagComplaint.after('<textarea id="'+tagComplaint.attr('id')+'" name="'+tagComplaint.attr('name')+'" rows="3">'+tagComplaint.val()+'</textarea>').remove()
    tagStatusLocalis.after('<textarea id="'+tagStatusLocalis.attr('id')+'" name="'+tagStatusLocalis.attr('name')+'" rows="3">'+tagStatusLocalis.val()+'</textarea>').remove()

  startDateTime = moment().format('DD.MM.YYYY HH:mm')

  initDatePicker = (selector, defaultDate) ->
    $el = $(selector)
    $el.on('dp.hide', () ->
      $el.find('input').change()
    )
    $el.datetimepicker
      format: 'DD.MM.YYYY HH:mm'
      useCurrent: no
      defaultDate: defaultDate

  initDatePicker('#startedAt', startDateTime)

  pageSize = 8
  $pagination = {}
  $paginationEl = $('#pagination')

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

  $patientForm.submit ->
    vm.isLoading(yes)
    if isPatientValid(vm.selected.patient)
      if formData
        formData.submit()
      else
        $patientForm.fileupload('send', {files: ''})
      yes
    else
      no

  checkUpData = null
  $checkUpForm = $('#medical-check-form')
  $checkUpForm.fileupload
    dataType: 'text'
    autoUpload: no
    replaceFileInput: yes
    singleFileUploads: no
    multipart: yes
    add: (e, data) ->
      checkUpData = data
    fail: (e, data) ->
      handleError(data.jqXHR)
      vm.isLoading(no)
    done: (e, data) ->
      result = data.result
      if result is 'OK'
        vm.isLoading(no)
        toastr.success('Yangi ma\'lumotlar ro\'yhatga olindi.')
        if vm.selected.patient.id()
          getPatientsCheckUps(vm.selected.patient.id())
        $addCheckUpModal.modal('hide')
      else
        alert(result or 'Tizimda xatolik! Iltimos qaytadan urinib ko\'ring.')

  defaultPatient =
    id: ''
    createdAt: ''
    firstName: ''
    lastName: ''
    middleName: ''
    gender: ''
    birthDate: ''
    age: ''
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
      passportNumber: ''
      province: ''
      street: ''
      home: ''
      work: ''
      position: ''
      bloodGroup: ''

  defaultCheckUp =
    id: ''
    patientId: ''
    userId: ''
    startedAt: ''
    finishedAt: ''
    complaint: ''
    objInfo: ''
    objReview: ''
    statusLocalis: ''
    diagnose: ''
    recommendation: ''

  vm = ko.mapping.fromJS
    isFiltersShown: no
    patients: []
    regions: []
    districts: []
    clientGroups: []
    bloodGroups: ['I(+)', 'I(-)', 'II(+)', 'II(-)', 'III(+)', 'III(-)', 'IV(+)', 'IV(-)']
    checkUps: []
    rightPage: 'summary'
    selected:
      patient: defaultPatient
      checkUp: defaultCheckUp
      districts: []
    filters:
      lastName: undefined
      isMale: yes
      isFemale: yes
      minAge: undefined
      maxAge: undefined
      regionId: undefined
      districtId: undefined
      clientGroupId: undefined
      passportNumber: undefined
      province: undefined
    checkUpFiles: []
    isLoading: no
    isNewPatient: no

  handleError = (error) ->
    vm.isLoading(no)
    if error.status is 401
      logout()
    else if error.status is 200 or error.status is 400 and error.responseText
      alert(error.responseText)
    else
      alert('Tizimda xatolik! Iltimos qaytadan urinib ko\'ring.')

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

  vm.formatDate = (millis, format = 'DD.MM.YYYY') ->
    if millis
      moment(millis).format(format)

  vm.getAge = (date) ->
    if date
      moment().diff(date, 'years')

  vm.onClose = ->
    vm.rightPage('summary')

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
      else if notvalid(patient.patientDataJson.passportNumber())
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
    getPatientsCheckUps(patient.id)
    vm.rightPage('cardIndex')

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
#      TODO use bootbox js
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

  vm.enableFilters = ->
    vm.isFiltersShown(!vm.isFiltersShown())

  vm.onFilterPatients = ->
    loadAllPatients()

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
      vm.patients patients

  vm.filters.lastName.subscribe ->
    loadAllPatients()

  vm.filters.isMale.subscribe ->
    loadAllPatients()

  vm.filters.isFemale.subscribe ->
    loadAllPatients()

  vm.filters.minAge.subscribe ->
    loadAllPatients()

  vm.filters.maxAge.subscribe ->
    loadAllPatients()

  vm.filters.regionId.subscribe ->
    loadAllPatients()

  vm.filters.districtId.subscribe ->
    loadAllPatients()

  vm.filters.clientGroupId.subscribe ->
    loadAllPatients()

  vm.filters.passportNumber.subscribe ->
    loadAllPatients()

  vm.filters.province.subscribe ->
    loadAllPatients()

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

  vm.getPatientFullName = ->
    "#{vm.selected.patient.lastName vm.selected.patient.firstName vm.selected.patient.middleName}"

  vm.selected.patient.regionId.subscribe (regionId) ->
    if regionId
      vm.selected.districts(ko.utils.arrayFilter(vm.districts(), (district) -> district.regionId is regionId))

  loadAllPatients()
  loadAllRegions()
  loadAllDistricts()
  loadAllClientGroups()

  # CHECH UP
  vm.onClickAddCheckUp = ->
    vm.selected.checkUp.startedAt(vm.formatDate(moment()))
    $addCheckUpModal.modal('show')

  isCheckUpValid = (checkUp) ->
    warningText =
#      if notvalid(checkUp.complaint())
#        'Shikoyat maydonini to\'ldiring!'
#      else
      if notvalid(checkUp.objInfo())
        'Obyektiv ma\'lumotlarni kiriting!'
      else if notvalid(checkUp.objReview())
        'Obyektiv ko\'rikni kiriting!'
#      else if notvalid(checkUp.statusLocalis())
#        'Status localis maydonini to\'ldiring!'
      else if notvalid(checkUp.diagnose())
        'Tashhis maydonini to\'ldiring!'
      else if notvalid(checkUp.recommendation())
        'Tavsiya maydonini to\'ldiring!'
      else
        isFilesInvalid = no
        for fileName in vm.checkUpFiles()
          fileName = fileName.toLowerCase()
          if !(/\.(jpg|jpeg|png|pdf|doc|docx|xls|xlsx|csv)$/.test(fileName))
            vm.checkUpFiles.removeAll()
            checkUpData = null
            isFilesInvalid = yes
            break
        if isFilesInvalid
          'Noto\'g\'ri fayl yuklandi. Iltimos fayllarni tekshirib qaytadan yuklang!'

    if warningText
      toastr.error(warningText)
      no
    else
      yes

  vm.onSubmitCheckUp = ->
    if isCheckUpValid(vm.selected.checkUp)
      vm.isLoading(yes)
      if checkUpData
        checkUpData.submit()
      else
        $checkUpForm.fileupload('send', {files: ''})

  getPatientsCheckUps = (patientId) ->
    $.get(apiUrl.checkUps + '/' + patientId)
    .fail handleError
    .done (checkUps) ->
      vm.checkUps checkUps

  vm.onCheckUpSelected = (checkUp) ->
    ko.mapping.fromJS(checkUp, {}, vm.selected.checkUp)

  vm.onFileSelected = (v, event) ->
    vm.checkUpFiles.removeAll()
    ko.utils.arrayForEach(event.target.files, (file) ->
      vm.checkUpFiles.push(file.name)
    )

  vm.fileUploadedInfo = ko.computed ->
    switch vm.checkUpFiles().length
      when 0 then 'Fayl yuklanmagan'
      when 1 then vm.checkUpFiles()[0]
      else vm.checkUpFiles().length + ' ta fayl yuklandi'

  Glob.vm = vm

  ko.applyBindings {vm}
