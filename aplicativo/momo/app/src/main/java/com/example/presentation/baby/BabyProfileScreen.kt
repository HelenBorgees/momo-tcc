package com.example.presentation.baby

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.presentation.components.MomoCard
import com.example.presentation.components.MomoPrimaryButton
import com.example.ui.theme.ApplicationBackground
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CriticalAlert
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryCoralLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftBorder
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BabyProfileScreen(
    onNavigateNext: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: BabyProfileViewModel = viewModel(),
    isEditing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPhotoPicked(context, uri)
        }
    }


    val calendar = Calendar.getInstance().apply {
        timeInMillis = uiState.birthDateMillis
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            viewModel.onBirthDateSelected(cal.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Perfil do Bebê" else "Cadastro do Bebê",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = PrimaryText
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ApplicationBackground
                )
            )
        },
        containerColor = ApplicationBackground,
        modifier = modifier.testTag("baby_profile_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Avatar Container with Photo Picker support
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(PrimaryCoralLight)
                        .border(2.dp, PrimaryCoral, CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!uiState.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = uiState.photoUri,
                            contentDescription = "Foto do bebê",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else if (uiState.name.isNotBlank()) {
                        Text(
                            text = uiState.name.take(1).uppercase(),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCoral
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChildCare,
                            contentDescription = "Foto do bebê",
                            tint = PrimaryCoral,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Camera / Edit badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PrimaryCoral)
                        .border(2.dp, ApplicationBackground, CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Selecionar foto",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action links: Adicionar/Alterar foto & Remover foto
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (uiState.photoUri.isNullOrBlank()) "Adicionar foto" else "Alterar foto",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryCoral,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                if (!uiState.photoUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Remover foto",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = CriticalAlert,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.onPhotoRemoved(context)
                            }

                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Suporte para 1 bebê no monitoramento",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(20.dp))

            MomoCard(
                shape = RoundedCornerShape(22.dp),
                contentPadding = 20.dp
            ) {
                // Name Field
                Text(
                    text = "Nome do Bebê *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onNameChange(it) },
                    placeholder = { Text("Nome da criança", fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = PrimaryCoral
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCoral,
                        unfocusedBorderColor = SoftBorder,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("baby_name_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Date of Birth Field (with auto age calculation)
                Text(
                    text = "Data de Nascimento *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                ) {
                    OutlinedTextField(
                        value = uiState.birthDateText,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        placeholder = { Text("Selecione a data", fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = PrimaryCoral
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = SoftBorder,
                            disabledTextColor = PrimaryText,
                            disabledContainerColor = CardSurface,
                            disabledLeadingIconColor = PrimaryCoral
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("baby_birthdate_input")
                    )
                }

                if (uiState.calculatedAge.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Idade calculada: ${uiState.calculatedAge}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryCoral
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Weight in Kg Field
                Text(
                    text = "Peso Atual (kg)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.weightKgText,
                    onValueChange = { viewModel.onWeightChange(it) },
                    placeholder = { Text("Ex: 11.0", fontSize = 14.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = PrimaryCoral
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCoral,
                        unfocusedBorderColor = SoftBorder,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("baby_weight_input")
                )
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.errorMessage ?: "",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CriticalAlert
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            MomoPrimaryButton(
                text = if (isEditing) "Salvar Alterações" else "Salvar e Continuar",
                onClick = {
                    viewModel.saveProfile {
                        onNavigateNext()
                    }
                },
                testTag = "baby_save_button"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
