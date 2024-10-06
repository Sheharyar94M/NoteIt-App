package playaxis.appinn.note_it.main.viewModels

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import playaxis.appinn.note_it.R
import kotlin.reflect.KClass

@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.viewModel(
    noinline provider: (SavedStateHandle) -> VM
) = createLazyViewModel(
    viewModelClass = VM::class,
    savedStateRegistryOwnerProducer = { this },
    viewModelStoreOwnerProducer = { this },
    viewModelProvider = provider
)

@MainThread
inline fun <reified VM : ViewModel> Fragment.viewModel(
    noinline provider: (SavedStateHandle) -> VM
) = createLazyViewModel(
    viewModelClass = VM::class,
    savedStateRegistryOwnerProducer = { this },
    viewModelStoreOwnerProducer = { this },
    viewModelProvider = provider
)

//@MainThread
//inline fun <reified VM : ViewModel> Fragment.navGraphParentViewModel(
//    @IdRes navGraphId: Int,
//    noinline provider: (SavedStateHandle) -> VM
//): Lazy<VM> {
//    val backStackEntry by lazy { findNavController().getBackStackEntry(navGraphId) }
//    return createLazyViewModel(
//        viewModelClass = VM::class,
//        savedStateRegistryOwnerProducer = { backStackEntry },
//        viewModelStoreOwnerProducer = { backStackEntry },
//        viewModelProvider = provider
//    )
//}

@MainThread
inline fun <reified VM : ViewModel> Fragment.navGraphViewModel(
    @IdRes navGraphId: Int,
    noinline provider: (SavedStateHandle) -> VM
): Lazy<VM> {
    val backStackEntry by lazy { findNavController().getBackStackEntry(navGraphId) }
//    val host = parentFragmentManager.findFragmentById(navHostId) as NavHostFragment
//    host.let {
//
//
//    }

//    val backStackEntry by lazy { host.navController.getBackStackEntry(navGraphId) }
    return createLazyViewModel(
        viewModelClass = VM::class,
        savedStateRegistryOwnerProducer = { backStackEntry },
        viewModelStoreOwnerProducer = { backStackEntry },
        viewModelProvider = provider
    )
}

@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.navGraphViewModel(
    @IdRes navGraphId: Int,
    @IdRes navHostId: Int = R.id.nav_host_main_fragment_container,
    noinline provider: (SavedStateHandle) -> VM
): Lazy<VM> {
    val backStackEntry by lazy { findNavController(navHostId).getBackStackEntry(navGraphId) }
    return createLazyViewModel(
        viewModelClass = VM::class,
        savedStateRegistryOwnerProducer = { backStackEntry },
        viewModelStoreOwnerProducer = { backStackEntry },
        viewModelProvider = provider
    )
}

fun <VM : ViewModel> createLazyViewModel(
    viewModelClass: KClass<VM>,
    savedStateRegistryOwnerProducer: () -> SavedStateRegistryOwner,
    viewModelStoreOwnerProducer: () -> ViewModelStoreOwner,
    viewModelProvider: (SavedStateHandle) -> VM
) = ViewModelLazy(viewModelClass, { viewModelStoreOwnerProducer().viewModelStore }, {
    object : AbstractSavedStateViewModelFactory(savedStateRegistryOwnerProducer(), Bundle()) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ) = viewModelProvider(handle) as T
    }
})

interface AssistedSavedStateViewModelFactory<T> {
    fun create(savedStateHandle: SavedStateHandle): T
}
