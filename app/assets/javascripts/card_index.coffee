$ ->
  my.initAjax()
  window.Glob ?= {}

  apiUrl =
    regions: '/home/regions'
    districts: '/home/districts'
    villages: '/home/villages'
    icds: '/home/icds'
    clientGroups: '/home/client-groups'
    bloodTypes: '/home/blood-types'
    patient: '/card-index/patient'
    patients: '/card-index/patients'
    checkUps: '/card-index/check-ups'
    supervisedOut: '/card-index/supervised-out'

  logout = ->
    window.alert("Your session has been expired!\nPlease log in.")
    my.navigateToUrl('/common/dashboard/logout')

  handleError = (error) ->
    vm.isLoading(no)
    if error.status is 401
      logout()
    else if error.status is 200 or error.status is 400 and error.responseText
      alert(error.responseText)
    else
      alert('Tizimda xatolik! Iltimos qaytadan urinib ko\'ring.')

  $patientModal = $('#patient-modal')
  $patientsFileModal = $('#patients-file-upload-modal')
  $checkUpModal = $('#check-up-modal')
  $supervisedOutModal = $('#supervised-out-modal')

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

  now = moment().format('DD.MM.YYYY HH:mm')

  initDatePicker = (selector, defaultDate, format) ->
    $el = $(selector)
    $el.on('dp.hide', () ->
      $el.find('input').change()
    )

    $el.datetimepicker
      format: format or 'DD.MM.YYYY HH:mm'
      useCurrent: no
      defaultDate: defaultDate

  formData = null
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
        toastr.success('Ma\'lumotlar muvaffaqiyatli ro\'yhatga olindi.')
        $patientModal.modal('hide')
        loadAllPatients()
      else
        alert(result or 'Tizimda xatolik! Iltimos qaytadan urinib ko\'ring.')

  checkUpData = null
  $checkUpForm = $('#check-up-form')
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
        toastr.success('Ma\'lumotlar ro\'yhatga olindi.')
        if vm.selected.patient.id()
          getPatientsCheckUps(vm.selected.patient.id())
        $checkUpModal.modal('hide')
      else
        alert(result or 'Tizimda xatolik! Iltimos qaytadan urinib ko\'ring.')

  patientsFileData = null
  $patientsFileForm = $('#patients-file-upload-form')
  $patientsFileForm.fileupload
    dataType: 'text'
    autoUpload: no
    singleFileUploads: true
    multipart: true
    add: (e, data) ->
      patientsFileData = data
      progress = parseInt(data.loaded / data.total * 100, 10)
      $('#progress .bar').css('width', progress + '%')
    progressall: (e, data) ->
      progress = parseInt(data.loaded / data.total * 100, 10)
      $('#progress .bar').css('width', progress + '%')
    fail: (e, data) ->
      $('#progress').hide()
      handleError(data.jqXHR)
    done: (e, data) ->
      $('#progress').hide()
      result = data.result
      if result is 'OK'
        toastr.success('Fayl muvaffaqiyatli yuklandi.')
        $patientsFileModal.modal('hide')
        vm.patientsFileUploadInfo('')
      else
        alert(result or 'Tizimda xatolik! Iltimos qaytadan urinib ko\'ring.')

  defaultPatient =
    id: ''
    createdAt: ''
    firstName: ''
    lastName: ''
    middleName: ''
    passportId: ''
    gender: ''
    birthDate: ''
    age: ''
    regionId: ''
    districtId: ''
    villageId: ''
    icd: ''
    clientGroup: ''
    village:
      id: ''
      name: ''
      districtId: ''
    patientDataJson:
      province: ''
      street: ''
      home: ''
      work: ''
      position: ''
      bloodType: ''
      email: ''
      phoneNumber: ''
    supervisedOutJson:
      date: ''
      reason: ''
      comments: ''

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
    receiveInfoJson:
      receiveType: ''
      receiveReason: ''

  PageName =
    Summary: 'summary'
    CardIndex: 'cardIndex'

  vm = ko.mapping.fromJS
    isFiltersShown: no
    patients: []
    regions: []
    districts: []
    villages: []
    icds: []
    clientGroups: []
    bloodTypes: []
    checkUps: []
    rightPage: PageName.Summary
    selected:
      patient: defaultPatient
      checkUp: defaultCheckUp
      districts: []
      villages: []
    filters:
      lastName: undefined
      isMale: yes
      isFemale: yes
      minAge: undefined
      maxAge: undefined
      regionId: undefined
      districtId: undefined
      villageId: undefined
      icd: undefined
      passportId: undefined
      province: undefined
    checkUpFiles: []
    patientsFileName: ''
    isLoading: no

  vm.PageName = PageName

  vm.formatDate = (millis, format = 'DD.MM.YYYY HH:mm') ->
    if millis
      moment(millis).format(format)

  vm.getAge = (date) ->
    if date
      moment().diff(date, 'years')

  vm.onClosePatientInfoPage = ->
    #    ko.mapping.fromJS(defaultPatient, {}, vm.selected.patient)
    vm.rightPage(PageName.Summary)
    console.log(vm.rightPage())

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
      else if notvalid(patient.icd())
        'Bemorning klient guruhini tanlang!'
      else if notvalid(patient.patientDataJson.bloodType())
        'Bemorning qon guruhini tanlang!'
      else if notvalid(patient.passportId())
        'Bemorning passport raqamini kiriting!'
      else if notvalid(patient.regionId())
        'Viloyatni tanlang!'
      else if notvalid(patient.districtId())
        'Tumanni tanlang!'
      else if notvalid(patient.patientDataJson.province())
        'Mahalla nomini kiriting!'
      else if notvalid(patient.patientDataJson.phoneNumber())
        'Bemorning telefon raqamini kiriting!'

    if warningText
      toastr.error(warningText)
      no
    else
      yes

  vm.onSubmitPatient = ->
    if isPatientValid(vm.selected.patient)
      vm.isLoading(yes)
      if formData
        formData.submit()
      else
        $patientForm.fileupload('send', {files: ''})
      yes
    else
      no

  vm.onPatientSelected = (patient) ->
    ko.mapping.fromJS(defaultPatient, {}, vm.selected.patient)
    ko.mapping.fromJS(patient, {}, vm.selected.patient)
    vm.selected.patient.birthDate(vm.formatDate(patient.birthDate, 'DD.MM.YYYY'))
    vm.rightPage(PageName.CardIndex)
    console.log(vm.rightPage())
    getPatientsCheckUps(patient.id)

  vm.onClickAddPatient = ->
    initDatePicker('#birthDate', '', 'DD.MM.YYYY')
    ko.mapping.fromJS(defaultPatient, {}, vm.selected.patient)
    $patientModal.modal('show')

  vm.onClickUploadPatients = ->
    $patientsFileModal.modal('show')

  vm.onClickEditPatient = ->
#    TODO Fix region and district select
    initDatePicker('#birthDate', '', 'DD.MM.YYYY')
    if vm.selected.patient.id()
      $patientModal.modal('show')
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

  vm.onSupervisedOutModalOpen = ->
    initDatePicker('#superviseDate', now)
    vm.selected.patient.supervisedOutJson.date(now)
    $supervisedOutModal.modal('show')

  vm.onSaveSupervisedOut = ->
    supObj = ko.mapping.toJS(vm.selected.patient.supervisedOutJson)
    errorText = if notvalid(supObj.date)
      'Iltimos sanani kiriting!'
    else if(notvalid(supObj.reason))
      'Iltimos nazoratdan chiqarish sababini belgilang!'
    else
      undefined

    if errorText
      toastr.error(errorText)
    else
      $.post(apiUrl.supervisedOut + '/' + vm.selected.patient.id(), JSON.stringify(supObj))
      .fail handleError
      .done (result) ->
        $supervisedOutModal.modal('hide')
        loadAllPatients()

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
#        if patient.supervisedOutJson?.date
#          patient.supervisedOutJson.date = vm.formatDate(parseInt(patient.supervisedOutJson.date))

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

  vm.filters.icd.subscribe ->
    loadAllPatients()

  vm.filters.passportId.subscribe ->
    loadAllPatients()

  vm.filters.province.subscribe ->
    loadAllPatients()

  loadAllRegions = ->
    $.get(apiUrl.regions).fail(handleError).done(vm.regions)

  loadAllDistricts = ->
    $.get(apiUrl.districts).fail(handleError).done(vm.districts)

  loadAllVillages = ->
    $.get(apiUrl.villages).fail(handleError).done(vm.villages)

  loadAllIcds = ->
    $.get(apiUrl.icds).fail(handleError).done(vm.icds)

  loadAllclientGroups = ->
    $.get(apiUrl.clientGroups).fail(handleError).done(vm.clientGroups)

  loadAllBloodTypes = ->
    $.get(apiUrl.bloodTypes).fail(handleError).done(vm.bloodTypes)

  vm.getPatientFullName = ->
    "#{vm.selected.patient.lastName vm.selected.patient.firstName vm.selected.patient.middleName}"

  vm.selected.patient.regionId.subscribe (regionId) ->
    if regionId
      vm.selected.districts(ko.utils.arrayFilter(vm.districts(), (district) -> district.regionId is regionId))
      vm.selected.villages([])

  vm.selected.patient.districtId.subscribe (districtId) ->
    if districtId
      vm.selected.villages(ko.utils.arrayFilter(vm.villages(), (village) -> village.districtId is districtId))

  vm.getDistrictById = (districtId) ->
    district = ko.utils.arrayFirst(vm.districts(), (district) -> district.id is districtId)
    console.log(district)
    if district
      district.name
    else
      ""

  loadAllPatients()
  loadAllRegions()
  loadAllDistricts()
  loadAllVillages()
  loadAllIcds()
  loadAllclientGroups()
  loadAllBloodTypes()

  # CHECH UP
  getPatientsCheckUps = (patientId) ->
    $.get(apiUrl.checkUps + '/' + patientId)
    .fail handleError
    .done (checkUps) ->
      vm.checkUps checkUps

  complaints = ['Umumiy holsizlik', 'Qabziyat', 'Bel-dumg\'aza sohasida og\'riq', 'Ishtaha pastligi', 'Epitsistostomik naycha borligi']
  statuses = ['Qorin simmetrik', 'Nafas aktida qatnashadi', 'Paypaslaganda yumshoq', 'Qorin pastki qismida epitsistostomik naycha', 'Funksiyasi saqlangan', 'Periferik l\tugunlari kattalashmagan']

  complaintTags = $('#complaint')
  statusLocalisTags = $('#statusLocalis')

  initTagPicker = (selector, source) ->
    $el = selector
    try
      $el.tag
        placeholder: '...'
        source: source
    catch e
      $el.after('<textarea id="'+$el.attr('id')+'" name="'+$el.attr('name')+'" rows="3">'+$el.val()+'</textarea>').remove()

  initTagPicker(complaintTags, complaints)
  initTagPicker(statusLocalisTags, statuses)

  refillTags = (el, tags) ->
    removeAllTags(el)
    tagObj = el.data('tag')
    for tag in tags
      tagObj.add(tag)

  removeAllTags = (el) ->
    tagObj = el.data('tag')
    while tagObj.values.length != 0
      tagObj.remove(0)

  vm.onClickAddCheckUp = ->
    removeAllTags(complaintTags)
    removeAllTags(statusLocalisTags)
    initDatePicker('#startedAt', now)
    ko.mapping.fromJS(defaultCheckUp, {}, vm.selected.checkUp)
    vm.selected.checkUp.startedAt(now)
    $checkUpModal.modal('show')

  vm.onCheckUpEdit = (isFinished) -> (checkUp) ->
    refillTags(complaintTags, checkUp.complaint.split(', '))
    refillTags(statusLocalisTags, checkUp.statusLocalis.split(', '))
    ko.mapping.fromJS(checkUp, {}, vm.selected.checkUp)
    if isFinished
      initDatePicker('#finishedAt', now)
      vm.selected.checkUp.finishedAt(now)
    initDatePicker('#startedAt', checkUp.startedAt)
    vm.selected.checkUp.startedAt(vm.formatDate(checkUp.startedAt))
    $checkUpModal.modal('show')

  isCheckUpValid = (checkUp) ->
    complaintsArr = complaintTags.data('tag').values
    statusLocalisArr = statusLocalisTags.data('tag').values
    warningText =
      if complaintsArr.length == 0
        'Shikoyat maydonini to\'ldiring!'
      else if notvalid(checkUp.objInfo())
        'Obyektiv ma\'lumotlarni kiriting!'
      else if notvalid(checkUp.objReview())
        'Obyektiv ko\'rikni kiriting!'
      else if statusLocalisArr.length == 0
        'Status localis maydonini to\'ldiring!'
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

#  Patients File Uploading
  vm.onPatientsFileSelected = (v, event) ->
    vm.patientsFileName(event.target.files[0].name)

  vm.onSubmitPatientsFile = ->
    if !(/\.(xls|xlsx)$/.test(vm.patientsFileName()))
      patientsFileData = null
      toastr.warning("Faqat XLS yoki XLSX formatdagi faylni yuklashingiz mumkin!")
    else
      if patientsFileData
        patientsFileData.submit()
        $('#progress .bar').css('width', 0)
        $('#progress').show()
      else
        toastr.warning('Iltimos faylni tanlang.')

  vm.onCancelPatientsFile = ->
    vm.patientsFileName('')

  Glob.vm = vm

  ko.applyBindings {vm}
