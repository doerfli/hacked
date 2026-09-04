package li.doerf.hacked.ui.screens.passwords

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.remote.pwnedpasswords.PwnedPassword
import li.doerf.hacked.util.Analytics

class PasswordViewModel(application: Application) : AndroidViewModel(application) {
    private val pwnedPassword = PwnedPassword()
    private val _state = MutableStateFlow<PasswordCheckState>(PasswordCheckState.Idle)
    val state: StateFlow<PasswordCheckState> = _state

    fun check(password: String) {
        _state.value = PasswordCheckState.Checking
        Analytics.trackCustomEvent(CustomEvent.CHECK_PASSWORD_PWNED)
        viewModelScope.launch {
            _state.value = when (val result = pwnedPassword.check(password)) {
                is PwnedPassword.Result.Safe -> {
                    Analytics.trackCustomEvent(CustomEvent.PASSWORD_NOT_PWNED)
                    PasswordCheckState.Safe
                }
                is PwnedPassword.Result.Pwned -> {
                    Analytics.trackCustomEvent(CustomEvent.PASSWORD_PWNED)
                    PasswordCheckState.Pwned(result.count)
                }
                is PwnedPassword.Result.Error -> {
                    Analytics.trackCustomEvent(CustomEvent.PASSWORD_PWNED_EXCEPTION)
                    PasswordCheckState.Error
                }
            }
        }
    }

    fun reset() {
        _state.value = PasswordCheckState.Idle
    }
}
