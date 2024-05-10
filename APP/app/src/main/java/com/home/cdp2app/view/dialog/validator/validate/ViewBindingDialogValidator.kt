package com.home.cdp2app.view.dialog.validator.validate

import androidx.viewbinding.ViewBinding
import com.home.cdp2app.view.dialog.validator.type.ValidateStatus

/**
 * 각 다이얼로그의 바인딩을 검사하는 validate interface 입니다.
 */
interface ViewBindingDialogValidator<T : ViewBinding> {

    /**
     * validate를 수행하는 메소드 입니다.
     * @param bind 검증할 뷰 파라미터 입니다.
     * @return Validate를 수행한 결과를 반환합니다.
     */
    fun validate(bind : T) : ValidateStatus

}