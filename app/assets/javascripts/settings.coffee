$ ->
  my.initAjax()
  window.Glob ?= {}

  apiUrl =
    user: '/settings/user'
    users: '/settings/users'
    regions: '/settings/regions'
    districts: '/settings/districts'
    departments: '/settings/departments'
    department: '/settings/department'

  handleError = (error) ->
    vm.isLoading(no)
    if error.status is 401
      logout()
    else if error.status is 200 or error.status is 400 and error.responseText
      alert(error.responseText)
    else
      alert('Something went wrong! Please try again.')

  $addUserModal = $('#add-user-modal')
  $editUserModal = $('#edit-user-modal')
  $changePasswordModal = $('#change-password-modal')

  $addDepartmentModal = $('#add-department-modal')
  $editDepartmentModal = $('#edit-department-modal')

  defaultDepartment =
    regionId: ''
    districtId: ''
    name: ''

  defaultUser =
    login: ''
    firstName: ''
    lastName: ''
    middleName: ''
    email: ''
    phoneNumber: ''
    roleCodes: 'super.user'

  vm = ko.mapping.fromJS
    users: []
    regions: []
    districts: []
    departments: []
    selected:
      user: defaultUser
      department: defaultDepartment
    isLoading: no

  notvalid = (str) ->
    !$.trim(str)

  isUserValid = (user) ->
    warningText =
      if notvalid(user.login)
        'Login maydonini to\'ldiring'
      else if notvalid(user.firstName)
        'Ism maydonini to\'ldiring'
      else if notvalid(user.lastName)
        'Please enter Last Name'
      else if user.login.indexOf(' ') isnt -1
        'Login should not contain spaces'
      else if user.email and !my.isValidEmail(user.email)
        'Please enter valid email address'
      else if user.phoneNumber and !my.isValidPhone(user.phoneNumber)
        'Please enter valid phone number'

    if warningText
      toastr.error(warningText)
      no
    else
      yes

  vm.addNewManager = ->
    userObj = ko.mapping.toJS(vm.selected.user)

    if isUserValid(userObj)
      vm.isLoading(yes)
      $.ajax
        url: apiUrl.user
        data: JSON.stringify(userObj)
        type: 'POST'
        dataType: "json"
        contentType: 'application/json'
      .fail handleError
      .done (id) ->
        vm.isLoading(no)
        userObj.id = id
        userObj.createdAt = +new Date
        alert('User account was successfully created and email with temporary password was sent')
        $addUserModal.modal('hide')

  vm.onClickAddUserButton = ->
    ko.mapping.fromJS(defaultUser, {}, vm.selected.user)
    $addUserModal.modal('show')

  vm.formatDate = (millis, format = 'MMM DD YYYY') ->
    if millis
      moment(millis).format(format)

  prettifyUsers = (rawUsers) ->
    for user in rawUsers
      user.roleCodesArr = []
      if user.roleCodes
        user.roleCodesArr = user.roleCodes.split(',')
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

  loadAllDepartments = ->
    $.get(apiUrl.departments)
    .fail handleError
    .done (departments) ->
      vm.departments departments

  vm.selected.department.regionId.subscribe (regionId) ->
    if regionId and !vm.selected.districtId
      $.get("#{apiUrl.districts}/#{regionId}")
      .fail handleError
      .done (districts) ->
        vm.districts districts

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
    depObj = ko.mapping.toJS(vm.selected.department)

    if isDepartmentValid(depObj)
      vm.isLoading(yes)
      $.ajax
        url: apiUrl.department
        data: JSON.stringify(depObj)
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
  loadAllDepartments()

  Glob.vm = vm

  ko.applyBindings {vm}
