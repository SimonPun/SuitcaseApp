plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.jivanpun.suitcaseapp"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.jivanpun.suitcaseapp"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation ("com.squareup.picasso:picasso:2.71828")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-auth-ktx:22.1.1")
    implementation("com.google.firebase:firebase-firestore:24.7.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.firebase:firebase-database:20.2.2")
    implementation("com.google.firebase:firebase-storage:20.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("com.google.android.gms:play-services-auth:20.6.0")
    implementation ("com.airbnb.android:lottie:3.4.4")
    implementation ("androidx.recyclerview:recyclerview:1.2.1")
    implementation ("androidx.recyclerview:recyclerview:your_recyclerview_version")
    implementation ("androidx.cardview:cardview:your_cardview_version")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation ("com.squareup.picasso:picasso:2.71828")
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    implementation ("com.google.android.material:material:1.5.0")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.afollestad.material-dialogs:core:3.3.0")

    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")


}