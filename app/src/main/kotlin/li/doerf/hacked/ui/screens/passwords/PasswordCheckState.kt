package li.doerf.hacked.ui.screens.passwords

sealed class PasswordCheckState {
    data object Idle : PasswordCheckState()
    data object Checking : PasswordCheckState()
    data object Safe : PasswordCheckState()
    data class Pwned(val count: Int) : PasswordCheckState()
    data object Error : PasswordCheckState()
}
