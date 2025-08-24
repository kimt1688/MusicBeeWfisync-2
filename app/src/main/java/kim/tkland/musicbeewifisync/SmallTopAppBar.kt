package kim.tkland.musicbeewifisync

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,

) {
    val scrollFraction = scrollBehavior?.state?.overlappedFraction ?: 0.0f
    val backgroundColor = colors.containerColor(scrollFraction)

    Surface(
        color = backgroundColor,
        modifier = modifier,
    ) {
        androidx.compose.material3.TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            modifier = Modifier.padding(contentPadding),
        )
    }
}

/*
@OptIn(ExperimentalMaterial3Api::class)
private fun TopAppBarColors.containerColor(scrollFraction: Float): Color {
    // Example: This is a simplified way to get the container color.
    // The actual TopAppBarColors might have a more direct way or you might
    // need to interpolate between `containerColor` and `scrolledContainerColor`.
    // The `this` keyword refers to the TopAppBarColors instance.
    // You'll need to implement the logic here based on how TopAppBarColors
    // provides its colors or how you want to calculate it.

    // A more correct approach if you want to mimic Material 3 behavior is to use
    // the functions provided by TopAppBarColors directly.
    // The original code was `colors.containerColor(...)` which implies `TopAppBarColors`
    // already has a method that could give you the color.
    // If `TopAppBarColors` has a function like `containerColor(scrollFraction: Float): Color`,
    // you might not even need this extension function.
    // However, the standard `TopAppBarColors` provides `containerColor` and `scrolledContainerColor`
    // as properties, not functions taking scrollFraction.
    // So, you might need to interpolate.

    // For simplicity, let's assume you want to use the default logic available
    // via the properties of TopAppBarColors. If you want a custom interpolation,
    // you'd do it here.
    // The Surface composable expects a Color.
    // The `TopAppBarColors.containerColor` is a property that returns a Color.
    // If you need to react to scrollFraction, you should use the one that changes with scroll.
    // This is typically handled by the `TopAppBar` itself if you provide it with `TopAppBarDefaults.topAppBarColors()`.

    // The Material 3 TopAppBarColors interface has:
    // val containerColor: Color
    // val scrolledContainerColor: Color
    // val navigationIconContentColor: Color
    // val titleContentColor: Color
    // val actionIconContentColor: Color

    // If you want to interpolate between containerColor and scrolledContainerColor:
    if (scrollFraction > 0.01f) { // Small threshold to consider it scrolled
        return this.scrolledContainerColor
    }
    return this.containerColor
}
*/

@OptIn(ExperimentalMaterial3Api::class)
private fun TopAppBarColors.containerColor(scrollFraction: Float): Color {
    // Interpolate between containerColor and scrolledContainerColor
    // based on the scrollFraction.
    // This is a common way to achieve the color transition.
    val colorTransitionFraction = scrollFraction.coerceIn(0f, 1f)

    // Simple linear interpolation
    // You might want a different interpolation based on your design needs
    return androidx.compose.ui.graphics.lerp(
        start = this.containerColor,
        stop = this.scrolledContainerColor,
        fraction = colorTransitionFraction
    )

    // Alternatively, a simpler threshold based approach if you prefer:
    // if (scrollFraction > 0.01f) { // Small threshold to consider it scrolled
    //     return this.scrolledContainerColor
    // }
    // return this.containerColor
}
