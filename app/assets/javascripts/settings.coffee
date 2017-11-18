$ ->
  my.initAjax()
  window.Glob ?= {}

  apiUrl =
    user: '/settings/user'
    users: '/settings/users'
    regions: '/settings/regions'
    districts: '/settings/districts'

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
    selected:
      user: defaultUser
      regionId: ''
      districtId: ''
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
    console.log(vm.selected.user)
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

  vm.selected.regionId.subscribe = (regionId) ->
    $.get("#{apiUrl.districts}/#{regionId}")
    .fail handleError
    .done (districts) ->
      vm.districts districts

  loadAllUsers()
  loadAllRegions()

  Glob.vm = vm

  ko.applyBindings {vm}
