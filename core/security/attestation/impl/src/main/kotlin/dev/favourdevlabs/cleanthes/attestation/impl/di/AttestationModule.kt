package dev.favourdevlabs.cleanthes.attestation.impl.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.favourdevlabs.cleanthes.attestation.api.KeyAttestationVerifier
import dev.favourdevlabs.cleanthes.attestation.impl.KeyAttestationVerifierImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class AttestationModule {
    @Binds
    abstract fun bindKeyAttestationVerifier(impl: KeyAttestationVerifierImpl): KeyAttestationVerifier
}
