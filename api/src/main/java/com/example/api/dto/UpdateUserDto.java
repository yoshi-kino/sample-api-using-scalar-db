package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonDeserialize(builder = UpdateUserDto.UpdateUserDtoBuilder.class)
public class UpdateUserDto {
  @JsonProperty("email")
  String email;

  @JsonProperty("family_name")
  String familyName;

  @JsonProperty("given_name")
  String givenName;

  @JsonProperty("user_detail")
  UserDetailDto userDetail;
}
