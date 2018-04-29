$ ->
  my.initAjax()
  window.Glob ?= {}

  apiUrl =
    user: '/settings/user'
    users: '/settings/users'
    regions: '/home/regions'
    districts: '/home/districts'
    departments: '/settings/departments'
    department: '/settings/department'
    roles: '/settings/roles'

  handleError = (error) ->
    vm.isLoading(no)
    if error.status is 401
      logout()
    else if error.status is 200 or error.status is 400 and error.responseText
      alert(error.responseText)
    else
      alert('Something went wrong! Please try again.')

#  $('.multiselect').multiselect(
#    nonSelectedText: 'Role tanlang'
#    allSelectedText: 'Hammasi tanlandi'
#    enableHTML: true
#    buttonClass: 'btn btn-white btn-primary'
#    templates:
#      button: '<button type="button" class="multiselect dropdown-toggle" data-toggle="dropdown"><span class="multiselect-selected-text"></span> &nbsp;<b class="fa fa-caret-down"></b></button>'
#      ul: '<ul class="multiselect-container dropdown-menu"></ul>'
#      li: '<li><a tabindex="0"><label></label></a></li>'
#      divider: '<li class="multiselect-item divider"></li>'
#      liGroup: '<li class="multiselect-item multiselect-group"><label></label></li>'
#  )

  $userModal = $('#user-modal')

  $addDepartmentModal = $('#add-department-modal')
  $editDepartmentModal = $('#edit-department-modal')

  defaultDepartment =
    regionId: ''
    districtId: ''
    name: ''

  defaultUser =
    id: undefined
    login: ''
    password: ''
    passwordConfirm: ''
    firstName: ''
    lastName: ''
    middleName: ''
    departmentId: ''
    email: ''
    phoneNumber: ''
    roleCodes: ''

  vm = ko.mapping.fromJS
    users: []
    regions: []
    districts: []
    departments: []
    roles: []
    selected:
      user: defaultUser
      department: defaultDepartment
      districts: []
    isLoading: no

  notvalid = (str) ->
    !$.trim(str)

  isUserValid = (user) ->
    warningText =
      if notvalid(user.login)
        'Login maydonini to\'ldiring'
      else if notvalid(user.password)
        'Parol maydonini to\'ldiring'
      else if notvalid(user.passwordConfirm)
        'Parolni takroran kiriting'
      else if notvalid(user.firstName)
        'Ism maydonini to\'ldiring'
      else if notvalid(user.lastName)
        'Familiya maydonini to\'ldiring'
      else if notvalid(user.middleName)
        'Otasining ismi maydonini to\'ldiring'
      else if notvalid(user.roleCodes)
        'Foydalanuvchi tipini tanglang'
      else if !user.departmentId
        'Tibbiy bo\'lim maydonini to\'ldiring'
      else if user.login.indexOf(' ') isnt -1
        'Login da bo\'sh joylar mavjud bo\'lmasligi kerak'
      else if user.email and !my.isValidEmail(user.email)
        'Haqiqiy email manzilini kiriting'
      else if user.phoneNumber and !my.isValidPhone(user.phoneNumber)
        'Haqiqiy telefon raqamni kiriting'
      else if user.password != user.passwordConfirm
        "Parollar bir-biriga mos kelmaydi! Iltimos parolni qaytadan kiriting"

    if warningText
      toastr.error(warningText)
      no
    else
      yes

  vm.onClickAddUserButton = ->
    ko.mapping.fromJS(defaultUser, {}, vm.selected.user)
    $userModal.modal('show')

  vm.onClickEditUserButton = (user) ->
    ko.mapping.fromJS(user, {}, vm.selected.user)
    $userModal.modal('show')

  vm.onSubmitUser = ->
    userObj = ko.mapping.toJS(vm.selected.user)
    console.log(userObj)
    if isUserValid(userObj)
      vm.isLoading(yes)
      $.ajax
        url: apiUrl.user
        data: JSON.stringify(userObj)
        type: 'POST'
        dataType: "json"
        contentType: 'application/json'
      .fail handleError
      .done (result) ->
        vm.isLoading(no)
        if userObj.id
          toastr.success("Foydalanuvchi ma'lumotlari muvaffaqiyatli saqlandi.")
        else
          toastr.success("Foydalanuvchi ma'lumotlari muvaffaqiyatli yaratildi.")
        loadAllUsers()
        $userModal.modal('hide')

  vm.formatDate = (millis, format = 'MMM DD YYYY') ->
    if millis
      moment(millis).format(format)

  prettifyUsers = (rawUsers) ->
    for user in rawUsers
      user.roleCodesArr = ''
      if user.roleCodes
        user.roleCodesArr = user.roleCodes.split(',')
      if user.department
        user.departmentName = user.department.name
#      for field in userProperties
#        user[field] ?= undefined
    rawUsers

  loadAllUsers = ->
    $.get(apiUrl.users)
    .fail handleError
    .done (users) ->
      vm.users prettifyUsers(users)

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

  loadAllDepartments = ->
    $.get(apiUrl.departments)
    .fail handleError
    .done (departments) ->
      for department in departments
        if department.region
          department.regionId = department.region.id
          department.regionName = department.region.name
        if department.district
          department.districtId = department.district.id
          department.districtName = department.district.name
      vm.departments departments

  loadAllRoles = ->
    $.get(apiUrl.roles)
    .fail handleError
    .done (roles) ->
      vm.roles roles

  vm.selected.department.regionId.subscribe (regionId) ->
    if regionId
      vm.selected.districts(ko.utils.arrayFilter(vm.districts(), (district) -> district.regionId is regionId))

  isDepartmentValid = (department) ->
    warningText =
      if !department.regionId
        'Viloyatni tanlang'
      else if !department.districtId
        'Tumanni tanlang'
      else if !my.hasText(department.name)
        'Tibbiy bo\'lim nomini kiriting'

    if warningText
      toastr.error(warningText)
      no
    else
      yes

  vm.onClickAddDepartmentButton = ->
    ko.mapping.fromJS(defaultDepartment, {}, vm.selected.department)
    $addDepartmentModal.modal('show')

  vm.createDepartment = ->
    userObj = ko.mapping.toJS(vm.selected.department)

    if isDepartmentValid(userObj)
      vm.isLoading(yes)
      $.ajax
        url: apiUrl.department
        data: JSON.stringify(userObj)
        type: 'POST'
        dataType: "json"
        contentType: 'application/json'
      .fail handleError
      .done (id) ->
        vm.isLoading(no)
        toastr.success('Tibbiy bo\'lim muvaffaqiyatli yaratildi')
        loadAllDepartments()
        $addDepartmentModal.modal('hide')

  vm.onClickEditDepartmentButton = (department) ->
    console.log(department)
    ko.mapping.fromJS(department, {}, vm.selected.department)
    $editDepartmentModal.modal('show')

  vm.updateDepartment = () ->
    depObj = ko.mapping.toJS(vm.selected.department)

    if isDepartmentValid(depObj)
      vm.isLoading(yes)
      $.ajax
        url: apiUrl.department + "/#{depObj.id}"
        data: JSON.stringify(depObj)
        type: 'PUT'
        dataType: "json"
        contentType: 'application/json'
      .fail handleError
      .done () ->
        vm.isLoading(no)
        toastr.success('Muvaffaqiyatli saqlandi')
        loadAllDepartments()
        $editDepartmentModal.modal('hide')

  vm.deleteDepartment = (department) ->
    if confirm('Bu tibbiy bo\'limni o\'chirishni xohlaysizmi?')
      $.ajax
        url: apiUrl.department + "/#{department.id}"
        type: 'DELETE'
        dataType: "json"
      .fail handleError
      .done () ->
        vm.departments.remove(department)
        toastr.success('Muvaffaqiyatli o\'chirildi')

  loadAllUsers()
  loadAllRegions()
  loadAllDistricts()
  loadAllDepartments()
  loadAllRoles()

  Glob.vm = vm

  ko.applyBindings {vm}
