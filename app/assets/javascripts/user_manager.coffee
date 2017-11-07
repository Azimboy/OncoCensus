$ ->
  my.initAjax()
  window.Glob ?= {}

  apiUrl =
    userManager:    '/admin/user-manager/user'
    apps:  '/admin/user-manager/apps'
    availableRoles: '/admin/user-manager/roles'
    changePassword: '/admin/user-manager/change-password'
    unblock: '/admin/user-manager/unblock'

  logout = ->
    window.alert("Your session has been expired!\nPlease log in.")
    window.location.href = '/admin/user-manager/logout'

  handleError = (error) ->
    vm.isLoading(no)
    if error.status is 401
      logout()
    else if error.status is 200 or error.status is 400 and error.responseText
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  $addUserModal = $('#add-user-modal')
  $editUserModal = $('#edit-user-modal')
  $changePasswordModal = $('#change-password-modal')
  $attestationReportModal = $('#attestation-report-modal')
  $unblockUserModal = $('#unblock-user-modal')

  $('body').on 'hidden.bs.modal', ->
    if $('.modal.in').length > 0
      $('body').addClass 'modal-open'
    return

  selectedManagerFromRow = {}
  userProperties = ['login', 'password', 'firstName', 'lastName', 'externalId', 'department', 'costCenter', 'costCenterManager', 'email', 'phoneNumber']

  defaultManager = {roleCodesArr: [], managedAppCodesArr: []}
  for field in userProperties
    defaultManager[field] ?= undefined

  vm = ko.mapping.fromJS
    managers: []
    apps: []
    filteredManagers: []
    appNameWithRoles: []
    roleCodeToRole: '' # map role code to app name
    availableRoleCodes: ''
    selected:
      user: defaultManager
    filter:
      name: ''
      login: ''
      role: ''
      acf2id: ''
    sortParam:
      refresh: ''
      fieldName: 'login'
      isAscending: yes
    isLoading: no
    attestationManagedAppCodes: []

  textContains = (txt, keyword) ->
    (txt or "").toLowerCase().indexOf(keyword) isnt -1

  roleNamesByRoleCodes = (roleCodesArr) ->
    for roleCode in roleCodesArr
      vm.availableRoleCodes()[roleCode]

  trimLowerCase = (str) -> $.trim(str).toLowerCase()

  ko.computed ->
    vm.sortParam.refresh() # to watch filter change
    fieldName = vm.sortParam.fieldName()
    direction = if vm.sortParam.isAscending() then 1 else -1
    vm.filteredManagers.sort (a, b) ->
      if !a[fieldName]
        1
      else if !b[fieldName]
        -1
      else
        direction * (a[fieldName] + '').localeCompare(b[fieldName] + '')

  ko.computed ->
    login = trimLowerCase(vm.filter.login())
    role = trimLowerCase(vm.filter.role())
    name = trimLowerCase(vm.filter.name())
    acf2id = trimLowerCase(vm.filter.acf2id())
    result = []
    for m in vm.managers()
      roleNamesStr = roleNamesByRoleCodes(m.roleCodesArr).join()
      if textContains(m.login, login) and textContains(roleNamesStr, role) and textContains(m.externalId, acf2id) and
          (textContains(m.firstName, name) or textContains(m.lastName, name))
        result.push m

    vm.filteredManagers(result)
    vm.sortParam.refresh +new Date

  fullfillCodes = (userObj) ->
    userObj.roleCodes =
      if userObj.roleCodesArr.length > 0
        userObj.roleCodesArr.join(',')
      else
        undefined
    userObj.expiresAt =
      if userObj.expiresAt
        userObj.expiresAt
      else
        new Date
    userObj

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
      else if notvalid(user.externalId)
        'Please enter ACF2 ID'
      else if notvalid(user.costCenter)
        'Please enter Cost Center'
      else if user.roleCodesArr.length is 0
        'Please select roles apps'
      else if user.login.indexOf(' ') isnt -1
        'Login should not contain spaces'
      else if user.email and !my.isValidEmail(user.email)
        'Please enter valid email address'
      else if user.phoneNumber and !my.isValidPhone(user.phoneNumber)
        'Please enter valid phone number'

    if warningText
      toastr.error warningText
      no
    else
      yes

  softTrim = (str) ->
    if typeof(str) is "boolean"
      str
    else
      s = $.trim(str)
      if s
        s

  trimFields = (user) ->
    for field in userProperties
      user[field] = softTrim(user[field])
    user

  vm.sortBy = (columnName) -> ->
    if columnName is vm.sortParam.fieldName()
      vm.sortParam.isAscending(!vm.sortParam.isAscending())
    else
      vm.sortParam.fieldName(columnName)

  vm.addNewManager = ->
    userObj = ko.mapping.toJS(vm.selected.user)
    userObj = fullfillCodes(userObj)
    userObj = trimFields(userObj)
    userObj.failedAttemptsCount = 0
    userObj.isFirstLogin = yes

    if !userObj.email
      toastr.error('Please enter email address')
    else if isUserValid(userObj)
      vm.isLoading(yes)
      $.ajax
        url: apiUrl.userManager
        data: JSON.stringify(userObj)
        type: 'POST'
        dataType: "json"
        contentType: 'application/json'
      .fail handleError
      .done (id) ->
        vm.isLoading(no)
        userObj.id = id
        userObj.createdAt = +new Date
        vm.managers.unshift userObj
        toastr.success('The email containing the password was sent and user successfully added')
        $addUserModal.modal('hide')

  vm.editManager = () ->
    userObj = ko.mapping.toJS(vm.selected.user)
    userObj = fullfillCodes(userObj)
    userObj = trimFields(userObj)

    if isUserValid(userObj)
      vm.isLoading(yes)
      $.ajax
        url: apiUrl.userManager + "/#{userObj.id}"
        data: JSON.stringify(userObj)
        type: 'PUT'
        dataType: "json"
        contentType: 'application/json'
      .fail handleError
      .done () ->
        vm.isLoading(no)
        userObj.updatedAt = +new Date
        vm.managers.replace selectedManagerFromRow, userObj
        toastr.success('Successfully saved')
        $editUserModal.modal('hide')

  vm.changePassword = () ->
    userObj = ko.mapping.toJS(vm.selected.user)
    password = $.trim(userObj.password)

    if !password
      toastr.error("Please enter password")
    else if password.indexOf(" ") isnt -1
      toastr.error('Password should not contain spaces')
    else
      $.ajax
        url: apiUrl.changePassword + "/#{userObj.id}"
        data: JSON.stringify(password: password)
        type: 'POST'
        dataType: "json"
        contentType: 'application/json'
      .fail handleError
      .done () ->
        toastr.success('Password has been changed successfully')
        $changePasswordModal.modal('hide')
        userObj.password = undefined
        userObj.updatedAt = +new Date
        vm.managers.replace selectedManagerFromRow, userObj

  vm.deleteManager = (manager) ->
    if confirm('Do you want to delete?')
      $.ajax
        url: apiUrl.userManager + "/#{manager.id}"
        type: 'DELETE'
        dataType: "json"
      .fail handleError
      .done () ->
        vm.managers.remove(manager)
        toastr.success('Successfully deleted')

  vm.onClickAddManagerButton = ->
    ko.mapping.fromJS(defaultManager, {}, vm.selected.user)
    $addUserModal.modal('show')

  vm.onClickDuplicateButton = (user) ->
    cloneDefaultManager = ko.mapping.toJS(ko.mapping.fromJS(defaultManager))
    cloneDefaultManager.roleCodesArr = getAvailableRoleCodes(user.roleCodesArr)
    ko.mapping.fromJS(cloneDefaultManager, {}, vm.selected.user)
    $addUserModal.modal('show')

  vm.onClickEditManagerButton = (user) ->
    selectedManagerFromRow = user
    ko.mapping.fromJS(user, {}, vm.selected.user)
    $editUserModal.modal('show')

  vm.onClickChangePasswordButton = (user) ->
    selectedManagerFromRow = user
    ko.mapping.fromJS(user, {}, vm.selected.user)
    $changePasswordModal.modal('show')

  vm.checkAllAppRoles = (appName) ->
    appNameWithRoles = ko.utils.arrayFirst(vm.appNameWithRoles(), (item) ->
      item.appName is appName
    )
    if appNameWithRoles
      rawRoleCodes = ko.utils.arrayMap(appNameWithRoles.roles, (role) -> role.code)
      roleCodes = ko.utils.arrayFilter(rawRoleCodes, (roleCode) ->
        vm.selected.user.roleCodesArr.indexOf(roleCode) is -1
      )
      setTimeout ->
        ko.utils.arrayPushAll(vm.selected.user.roleCodesArr, roleCodes)
      , 10

  vm.getCheckAllLabel = (appNameWithRoles) ->
    appName = appNameWithRoles.appName
    if appNameWithRoles.roles.length > 1
      """<a class='pull-right' onclick='Glob.vm.checkAllAppRoles("#{appName}")'>select all</a> #{appName}"""
    else
      appName

  vm.getAttestationReport = ->
    vm.attestationManagedAppCodes(vm.selected.user.managedAppCodesArr())
    if vm.attestationManagedAppCodes().length == 0
      toastr.error('Please select app for attestation report.')
      no
    else
      $attestationReportModal.modal('hide')
      yes

  vm.onAttestationReport = ->
    ko.mapping.fromJS(defaultManager, {}, vm.selected.user)
    vm.checkAllApps()
    $attestationReportModal.modal('show')

  vm.checkAllApps = ->
    rawAppCodes = ko.utils.arrayMap(vm.apps(), (code) -> code.code)
    appCodes = ko.utils.arrayFilter(rawAppCodes, (appCode) ->
      vm.selected.user.managedAppCodesArr.indexOf(appCode) is -1
    )
    setTimeout ->
      ko.utils.arrayPushAll(vm.selected.user.managedAppCodesArr, appCodes)
    , 10

  vm.unCheckAllApps = ->
    setTimeout ->
      vm.selected.user.managedAppCodesArr.removeAll()
    , 10

  vm.getCheckAllApps = ->
    """<a class='pull-right' onclick='Glob.vm.unCheckAllApps()'>Unselect All</a>
       <span class='pull-right'>&nbsp; &nbsp; &nbsp;</span>
       <a class='pull-right' onclick='Glob.vm.checkAllApps()'>Select All</a>
       Apps"""

  vm.getAppNameWithRoleNames = (roleCodes) -> ko.computed ->
    roleCodesToAppNameWithRoles(roleCodes)

  vm.getRolesCount = (roleCodes) -> ko.computed ->
    arr = roleCodesToAppNameWithRoles(roleCodes)
    rolesCount = 0
    if arr.length > 0
      for app in arr
        rolesCount += app.roles.length
    rolesCount

  vm.getRolesCountWithAppNames = (roleCodes) -> ko.computed ->
    arr = roleCodesToAppNameWithRoles(roleCodes)
    if arr.length > 0
      rolesCount = vm.getRolesCount(roleCodes)()
      appNames = ko.utils.arrayMap(arr, (a) -> a.appName)
      "#{rolesCount} roles in #{appNames.join(', ')}"

  vm.getRoleNameWithAppName = (roleCodes) -> ko.computed ->
    for roleCode in roleCodes
      role = vm.roleCodeToRole()[roleCode]
      if role
        return "#{role.name} in #{role.app.name}"

  vm.foldRoles = (status) -> (a, event) ->
    $(event.currentTarget).closest('table').find('.role-app-name').collapse(status)

  vm.formatDate = (millis, format = 'MMM DD YYYY') ->
    if millis
      moment(millis).format(format)

  vm.onClickUnblockButton = (user) ->
    selectedManagerFromRow = user
    ko.mapping.fromJS(user, {}, vm.selected.user)
    $unblockUserModal.modal('show')

  $busyLoader = $('.busy_loader')
  vm.unblockUser = () ->
    userObj = ko.mapping.toJS(vm.selected.user)
    vm.isLoading(yes)
    $busyLoader.show()
    $.ajax
      url: apiUrl.unblock + "/#{userObj.id}"
      type: 'GET'
      dataType: "json"
    .fail handleError
    .done (response) ->
      vm.isLoading(no)
      if response.isSent
        toastr.success("Successfully unblocked and reset password email was successfully sent.")
        loadManagers()
        $unblockUserModal.modal('hide')
      else
        toastr.error(response.failReason)
    .always ->
      $busyLoader.hide()

  getAvailableRoleCodes = (roleCodes) ->
    ko.utils.arrayFilter(roleCodes, (roleCode) -> vm.roleCodeToRole()[roleCode])

  roleCodesToAppNameWithRoles = (roleCodes) ->
    roles =
      for roleCode in roleCodes
        vm.roleCodeToRole()[roleCode]
    getAppNameWithRoles(ko.utils.arrayFilter(roles, (role) -> role))

  getCodeNameMap = (arr) ->
    obj = {}
    for item in arr
      obj[item.code] = item.name
    obj

  getAppNameWithRoles = (roles) ->
    appNameWithRoles = {}
    for role in roles
      appName = role.app.name
      appNameWithRoles[appName] ?= []
      appNameWithRoles[appName].push(role)
    arr = $.map(appNameWithRoles, (value, key) ->
      {appName: key, roles: value}
    )
    arr.sort((a, b) -> a.appName.localeCompare(b.appName))

  prettifyUsers = (rawUsers) ->
    for user in rawUsers
      user.roleCodesArr = []
      if user.roleCodes
        user.roleCodesArr = user.roleCodes.split(',')

      if user.specPart
        $.extend user, JSON.parse(user.specPart)
      for field in userProperties
        user[field] ?= undefined
    rawUsers

  loadManagers = ->
    $.get(apiUrl.userManager)
    .fail handleError
    .done (users) ->
      vm.managers prettifyUsers(users)

  loadAvailableApps = ->
    $.get(apiUrl.apps)
      .fail handleError
      .done (response) ->
        vm.apps response

  loadAvailableRoles = ->
    $.get(apiUrl.availableRoles)
    .fail handleError
    .done (response) ->
      roleCodeToRole = {}
      for role in response
        roleCodeToRole[role.code] = role
      vm.availableRoleCodes getCodeNameMap(response)
      vm.roleCodeToRole(roleCodeToRole)
      vm.appNameWithRoles getAppNameWithRoles(response)


  $(document).on('shown.bs.collapse', '.collapse', ->
    $(@)
    .parent()
    .find(".glyphicon")
    .removeClass("glyphicon-chevron-down")
    .addClass("glyphicon-chevron-up")
  )

  $(document).on('hidden.bs.collapse', '.collapse', ->
    $(@)
    .parent()
    .find(".glyphicon")
    .removeClass("glyphicon-chevron-up")
    .addClass("glyphicon-chevron-down");
  )

  Glob.vm = vm

  loadAvailableRoles()
  loadAvailableApps()
  loadManagers()

  ko.applyBindings {vm}
