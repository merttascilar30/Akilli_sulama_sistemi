package com.example.backend.service;

import com.example.backend.dto.IstasyonRequestDto;
import com.example.backend.dto.IstasyonResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Istasyonlarla ilgili is mantigini tanimlayan servis arayuzu.
 */
public interface IstasyonService {

    IstasyonResponseDto istasyonEkle(IstasyonRequestDto istasyonRequestDto);

    IstasyonResponseDto istasyonGuncelle(UUID id, IstasyonRequestDto istasyonRequestDto);

    List<IstasyonResponseDto> tumIstasyonlariListele();

    IstasyonResponseDto istasyonGetir(UUID id);
}
