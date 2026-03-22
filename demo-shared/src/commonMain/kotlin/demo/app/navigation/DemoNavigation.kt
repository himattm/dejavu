package demo.app.navigation

import androidx.compose.runtime.Composable
import demo.app.ui.*

enum class DemoScreen(val title: String, val content: @Composable () -> Unit) {
    Counter("Counter", { CounterScreen() }),
    ProductList("Product List", { ProductListScreen() }),
    LazyListStress("Lazy List Stress", { LazyListStressScreen() }),
    AnimationStress("Animation Stress", { AnimationStressScreen() }),
    DeepNestingStress("Deep Nesting Stress", { DeepNestingStressScreen() }),
    SharedStateStress("Shared State Stress", { SharedStateStressScreen() }),
    FlowState("Flow State", { FlowStateScreen() }),
    KeyIdentity("Key Identity", { KeyIdentityScreen() }),
    DialogPopup("Dialog & Popup", { DialogPopupScreen() }),
    Subcompose("Subcompose", { SubcomposeScreenRoot() }),
    InputScroll("Input & Scroll", { InputScrollScreen() }),
    LazyVariants("Lazy Variants", { LazyVariantsScreen() }),
    PagerCrossfade("Pager & Crossfade", { PagerCrossfadeScreen() }),
    AdvancedPatterns("Advanced Patterns", { AdvancedPatternsScreen() }),
    ScaffoldSlots("Scaffold Slots", { ScaffoldSlotsScreen() }),
    ToggleMorph("Toggle Morph", { ToggleMorphScreen() }),
    ChipFilter("Chip Filter", { ChipFilterScreen() }),
    ExpandableCard("Expandable Card", { AccordionListScreen() }),
    StarRating("Star Rating", { RatingBarScreen() }),
    DonutChart("Donut Chart", { DonutChartScreen() }),
    CollapsingHeader("Collapsing Header", { CollapsingHeaderScreen() }),
    SwipeList("Swipe List", { SwipeListScreen() }),
    ReorderList("Reorder List", { ReorderListScreen() }),
}
