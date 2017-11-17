$ ->
  my.initAjax()
  window.Glob ?= {}

  apiUrl =
    user: '/settings/user'
    users: '/settings/users'

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
    email: ''
    phoneNumber: ''
    roleCodes: ['super.user']

  vm = ko.mapping.fromJS
    users: []
    selected:
      user: defaultUser
    isLoading: no

  notvalid = (str) ->
    !$.trim(str)

  isUserValid = (user) ->
    warningText =
      if notvalid(user.login)
        'Please enter Login'
      else if notvalid(user.firstName)
        'Please enter First Name'
      else if notvalid(user.lastName)
        'Please enter Last Name'
      else if user.login.indexOf(' ') isnt -1
        'Login should not contain spaces'
      else if user.email and !my.isValidEmail(user.email)
        'Please enter valid email address'
      else if user.phoneNumber and !my.isValidPhone(user.phoneNumber)
        'Please enter valid phone number'

    if warningText
      alert(warningText)
      no
    else
      yes

  vm.addNewManager = ->
    userObj = ko.mapping.toJS(vm.selected.user)

    if !userObj.email
      alert('Email manzilini kiriting')
    else if isUserValid(userObj)
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

  #  softTrim = (str) ->
#    if typeof(str) is "boolean"
#      str
#    else
#      s = $.trim(str)
#      if s
#        s

#  trimFields = (user) ->
#    for field in userProperties
#      user[field] = softTrim(user[field])
#    user

  #  vm.editManager = () ->
#    userObj = ko.mapping.toJS(vm.selected.user)
#
#    userObj = fullfillCodes(userObj)
#    userObj = trimFields(userObj)
#
#    if isUserValid(userObj)
#      isManager = userObj.managedAppCodesArr.length > 0
#      if !isManager or confirm("You've selected one or more Managed Apps.\n\nIt means that the user will be a User Manager," +
#              " which will be able to create/edit user accounts for the selected Apps.\n\nAre you sure you want to do it?")
#        vm.isLoading(yes)
#        $.ajax
#          url: apiUrl.userManager + "/#{userObj.id}"
#          data: JSON.stringify(userObj)
#          type: 'PUT'
#          dataType: "json"
#          contentType: 'application/json'
#        .fail handleError
#        .done () ->
#          vm.isLoading(no)
#          userObj.updatedAt = +new Date
#          if userObj.isInternal && !selectedManagerFromRow.isInternal
#            vm.managers.remove(selectedManagerFromRow)
#          else if !userObj.isInternal && selectedManagerFromRow.isInternal
#            vm.managers.unshift userObj
#            vm.internalUsers.remove(selectedManagerFromRow)
#          else
#            vm.managers.replace selectedManagerFromRow, userObj
#            vm.internalUsers.replace selectedManagerFromRow, userObj
#
#          toastr.success('Successfully saved')
#          $editUserModal.modal('hide')
#
#  vm.changePassword = () ->
#    userObj = ko.mapping.toJS(vm.selected.user)
#    password = $.trim(userObj.password)
#
#    if !password
#      toastr.error("Please enter password")
#    else if password.indexOf(" ") isnt -1
#      toastr.error('Password should not contain spaces')
#    else
#      $.ajax
#        url: apiUrl.changePassword + "/#{userObj.id}"
#        data: JSON.stringify(password: password)
#        type: 'POST'
#        dataType: "json"
#        contentType: 'application/json'
#      .fail handleError
#      .done () ->
#        toastr.success('Password has been changed successfully')
#        $changePasswordModal.modal('hide')
#        userObj.password = undefined
#        userObj.updatedAt = +new Date
#        vm.managers.replace selectedManagerFromRow, userObj
#        vm.internalUsers.replace selectedManagerFromRow, userObj
#
#  vm.deleteManager = (manager) ->
#    if confirm('Do you want to delete?')
#      $.ajax
#        url: apiUrl.userManager + "/#{manager.id}"
#        type: 'DELETE'
#        dataType: "json"
#      .fail handleError
#      .done () ->
#        vm.managers.remove(manager)
#        vm.internalUsers.remove(manager)
#        toastr.success('Successfully deleted')

#  vm.onClickEditManagerButton = (user) ->
#    selectedManagerFromRow = user
#    ko.mapping.fromJS(user, {}, vm.selected.user)
#    $editUserModal.modal('show')
#
#  vm.onClickChangePasswordButton = (user) ->
#    selectedManagerFromRow = user
#    ko.mapping.fromJS(user, {}, vm.selected.user)
#    $changePasswordModal.modal('show')
#
#  vm.openDeletedUsersModal = ->
#    loadDeletedUsers(vm.selected.clientCode())
#    $deletedUsersModal.modal('show')

  loadAllUsers = ->
    $.get(apiUrl.users)
    .fail handleError
    .done (users) ->
      vm.users prettifyUsers(users)

  loadAllUsers()

  Glob.vm = vm

  ko.applyBindings {vm}
