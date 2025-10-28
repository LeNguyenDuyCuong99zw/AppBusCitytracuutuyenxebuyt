package com.map.buscity.ui.splash
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.map.buscity.R

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val background = Color(0xFF63EE83)

    Box(
        modifier = modifier
            .background(background)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.35f
            val cx = -radius * 0.07f
            val cy = radius * 1.7f
            drawCircle(
                color = Color(0xFF8BF8A5),
                center = Offset(cx, cy),
                radius = radius
            )
        }
            Image(
                painter = painterResource(id = R.drawable.tron1),
                contentDescription = "Top-right rings",
                modifier = Modifier
                    .size(width = 140.dp, height = 240.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = 50.dp),
                contentScale = ContentScale.FillHeight
            )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row with rounded logo container and title image
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_bus),
                        contentDescription = "Logo bus",
                        modifier = Modifier.size(130.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Image(
                    painter = painterResource(id = R.drawable.text_bus),
                    contentDescription = "Bus City text",
                    modifier = Modifier.size(160.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tra cứu nhanh – Lên xe liền",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(y = (-40).dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            //(hoa tiet)
            Image(
                painter = painterResource(id = R.drawable.hoatiet1),
                contentDescription = "Decorative lines",
                modifier = Modifier
                    .size(width = 160.dp, height = 180.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-60).dp, y = (-170).dp)
            )
            Image(
                painter = painterResource(id = R.drawable.blob1),
                contentDescription = "Blob decorative",
                modifier = Modifier
                    .size(width = 640.dp, height = 480.dp)
                    .align(Alignment.BottomEnd)
                    // place behind other siblings (like the Column with text)
                    .zIndex(-1f)
                    // nudge the blob slightly down so it sits lower behind content
                    .offset(x = (150).dp, y = (-150).dp),
                contentScale = ContentScale.FillHeight
            )

            // Increase illustration height so it's fully visible above system navigation
            CityIllustration(height = 92.dp)
        }
    }
}

@Composable
fun CityIllustration(height: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.back1),
        contentDescription = "City illustration",
        modifier = modifier
            .fillMaxWidth()
            // Let the image keep its intrinsic aspect and avoid being cropped.
            .wrapContentHeight(align = Alignment.Bottom)
            // Add a little bottom padding so it's not under the nav bar on some devices.
            .padding(bottom = 5.dp),
        contentScale = ContentScale.FillWidth
    )
}
